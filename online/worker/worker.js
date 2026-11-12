// Hidden Leaf Online: the rooms of Hidden Leaf (Naruto Shippuden: Ultimate Ninja 5, SLES-55605, game id "un5") over the
// internet. A Cloudflare Worker with a D1 database (schema.sql), made from Dragon Rage's Tag Team Online Worker
// (port/tagnet-worker there). It never carries the game itself: it gives a room a six-digit code, tells the two players
// each other's addresses (what each phone found out about itself: its own network and STUN), and passes the match's
// packets on through the room's own relay (a Durable Object, below) when the two phones cannot reach each other directly.
// The match goes between the phones over UDP (recomp/android/host/online/link.cpp), through the relay only when it must.
//
// Requests: POST /v1/<what> with a JSON body; answers are JSON (errors: {error: "..."} with a 4xx status).
//   /v1/hello  {v, game}                                -> {ok, v, game, colo, country, time}   (the app's connection test)
//   /v1/host   {v, game, fp, name, addrs, opts}         -> {code, key, you}
//   /v1/join   {v, game, fp, code, name, addrs}         -> {key, you, host: {name, addrs, seen}, opts}
//                                                          ("no room", "version", "game", "different", "full")
//   /v1/poll   {code, key}                             -> host: {guest: {name, addrs, seen} | null, opts}
//                                                         guest: {host: {name, addrs, seen}, opts}   ("no room": it is gone)
//   /v1/addrs  {code, key, addrs}                      -> {}   (one's addresses again)
//   /v1/relay  {code, key}                             -> {turn: {server, username, credential}} | {noRelay: why}   (host only)
//   /v1/leave  {code, key}                             -> {}   (the host leaving closes the room; the guest leaving frees it)
//   GET /v1/ws?code=&key=   (a WebSocket)              the room's relay: the room's own Durable Object (HiddenLeafRelay)
//                      passes binary messages between the host and the guest, as they are.
// `v` is the network version (HL_NET_VERSION in recomp/android/host/online/net_version.h and Online.NET_VERSION in the app):
// rooms only take games of their own version and of the same game. `fp` is what must be the same on both phones for the
// two games to stay the same (the app's build and the mods switched on): a room only takes a guest with the host's.
// `addrs` is a list of up to eight "ip:port"; `seen` is the address this Worker saw a player under. `opts` are the host's
// choices for the match ({delay: 0 automatic or 1-4 frames, relay: true/false}). A room nobody asked about for
// ROOM_IDLE seconds is gone.
//
// Settings (Cloudflare dashboard or wrangler.jsonc, the Worker's variables and secrets):
//   DB                 the D1 database (binding)
//   RELAY              the Durable Object of the rooms' relay (binding, class HiddenLeafRelay)
//   ALLOWED_VERSIONS   the network versions taken, "1" or "1,2" (default: any)
//   ROOMS_PER_ADDRESS  rooms one address may have open at once (default 4)
//   TURN_KEY_ID, TURN_KEY_TOKEN   optional secrets: a TURN key of Cloudflare's (Realtime > TURN), for /v1/relay
//   TURN_DAILY_MAX     TURN logins a day, all rooms together (default 300)

export const GAME = "un5";
const ROOM_IDLE = 180;       // seconds a room lives with nobody asking about it
const RELAY_PER_DAY = 300;
const MAX_BODY = 4096;
const MAX_RELAY_MESSAGE = 1600;

function reply(obj, status = 200) {
    return new Response(JSON.stringify(obj), { status, headers: { "content-type": "application/json", "cache-control": "no-store" } });
}

function cleanName(s) {
    return typeof s === "string" ? s.replace(/[\u0000-\u001f"\\]/g, "").trim().slice(0, 16) : "";
}

function cleanAddrs(a) {
    if (!Array.isArray(a)) return [];
    return a.filter((x) => typeof x === "string" && /^[0-9a-fA-F.:\[\]]{3,60}$/.test(x)).slice(0, 8);
}

function cleanFp(s) {
    return typeof s === "string" ? s.replace(/[^0-9A-Za-z._:+-]/g, "").slice(0, 96) : "";
}

function cleanOpts(o) {
    const out = { delay: 0, relay: true };
    if (o && typeof o === "object" && !Array.isArray(o)) {
        if (Number.isInteger(o.delay) && o.delay >= 0 && o.delay <= 4) out.delay = o.delay;
        if (typeof o.relay === "boolean") out.relay = o.relay;
    }
    return out;
}

export function versionAllowed(env, v) {
    if (!Number.isInteger(v)) return false;
    const list = String(env.ALLOWED_VERSIONS ?? "").split(",").map((x) => Number.parseInt(x, 10)).filter(Number.isInteger);
    return list.length === 0 || list.includes(v);
}

function randomKey() {
    const b = new Uint8Array(16);
    crypto.getRandomValues(b);
    return [...b].map((x) => x.toString(16).padStart(2, "0")).join("");
}

function randomCode() {
    const b = new Uint32Array(1);
    crypto.getRandomValues(b);
    return String(100000 + (b[0] % 900000)); // six digits, easy to read out over a call
}

async function sweep(env, now) {
    await env.DB.prepare("DELETE FROM hl_rooms WHERE touched < ?").bind(now - ROOM_IDLE).run();
}

// Who asks: the room's host or its guest, by the key each was given.
async function whoIs(env, code, key) {
    if (typeof code !== "string" || typeof key !== "string" || key.length < 16) return null;
    const room = await env.DB.prepare("SELECT * FROM hl_rooms WHERE code = ?").bind(code).first();
    if (!room) return null;
    if (room.host_key === key) return { room, host: true };
    if (room.guest_key && room.guest_key === key) return { room, host: false };
    return null;
}

function hostOf(room) {
    return { name: room.host_name, addrs: JSON.parse(room.host_addrs), seen: room.host_seen };
}

function guestOf(room) {
    return room.guest_key ? { name: room.guest_name ?? "", addrs: JSON.parse(room.guest_addrs ?? "[]"), seen: room.guest_seen ?? "" } : null;
}

// Cloudflare's TURN (Realtime > TURN): a login good for three hours, from the key the Worker keeps as secrets.
async function relayLogin(env, now) {
    if (!env.TURN_KEY_ID || !env.TURN_KEY_TOKEN) return { why: "this server has no TURN key" };
    const day = now - (now % 86400);
    const row = await env.DB.prepare("INSERT INTO hl_relay_days (day, n) VALUES (?, 1) ON CONFLICT(day) DO UPDATE SET n = n + 1 RETURNING n").bind(day).first();
    if (row && row.n > (Number(env.TURN_DAILY_MAX) || RELAY_PER_DAY)) return { why: "the relay's logins for today are used up" };
    try {
        const r = await fetch(`https://rtc.live.cloudflare.com/v1/turn/keys/${env.TURN_KEY_ID}/credentials/generate-ice-servers`, {
            method: "POST",
            headers: { authorization: `Bearer ${env.TURN_KEY_TOKEN}`, "content-type": "application/json" },
            body: JSON.stringify({ ttl: 10800 }),
        });
        if (!r.ok) return { why: `the TURN service answered ${r.status}` };
        const j = await r.json();
        const list = Array.isArray(j.iceServers) ? j.iceServers : j.iceServers ? [j.iceServers] : [];
        for (const s of list) {
            const urls = Array.isArray(s.urls) ? s.urls : [s.urls];
            const udp = urls.find((u) => typeof u === "string" && u.startsWith("turn:") && u.includes("transport=udp"));
            if (udp && s.username && s.credential) return { server: udp.slice(5).split("?")[0], username: s.username, credential: s.credential };
        }
        return { why: "the TURN service's answer had no UDP relay in it" };
    } catch (e) {
        return { why: "the TURN service could not be asked" };
    }
}

export async function handle(request, env, now) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/") {
        return new Response("Hidden Leaf Online: rooms for Hidden Leaf (Naruto Shippuden: Ultimate Ninja 5).\n", { headers: { "content-type": "text/plain; charset=utf-8" } });
    }
    if (request.method === "GET" && url.pathname === "/v1/ws") {
        if (request.headers.get("upgrade") !== "websocket") return reply({ error: "bad request" }, 426);
        if (!env.RELAY) return reply({ error: "no relay" }, 503);
        await sweep(env, now);
        const who = await whoIs(env, url.searchParams.get("code"), url.searchParams.get("key"));
        if (!who) return reply({ error: "no room" }, 404);
        await env.DB.prepare("UPDATE hl_rooms SET touched = ? WHERE code = ?").bind(now, who.room.code).run();
        const headers = new Headers(request.headers);
        headers.set("x-hl-who", who.host ? "h" : "g");
        // The relay is made near whoever connects first (left to Cloudflare it may sit on another continent: 100-150 ms more
        // each way between two players in Brazil, as Dragon Rage measured).
        const hints = { SA: "sam", NA: "enam", EU: "weur", AS: "apac", OC: "oc", AF: "afr" };
        const hint = hints[(request.cf && request.cf.continent) || ""] || "sam";
        const stub = env.RELAY.get(env.RELAY.idFromName(who.room.code), { locationHint: hint });
        return stub.fetch(new Request(request.url, { headers }));
    }
    if (request.method !== "POST" || !url.pathname.startsWith("/v1/")) return reply({ error: "bad request" }, 404);
    let body;
    try {
        const raw = await request.arrayBuffer();
        if (raw.byteLength > MAX_BODY) return reply({ error: "bad request" }, 413);
        body = JSON.parse(new TextDecoder().decode(raw));
    } catch (e) {
        return reply({ error: "bad request" }, 400);
    }
    if (!body || typeof body !== "object" || Array.isArray(body)) return reply({ error: "bad request" }, 400);
    const what = url.pathname.slice(4);
    const ip = request.headers.get("cf-connecting-ip") ?? "";

    if (what === "hello") {
        const cf = request.cf || {};
        return reply({ ok: versionAllowed(env, body.v) && body.game === GAME, v: body.v ?? null, game: GAME, colo: cf.colo ?? "", country: cf.country ?? "", time: now });
    }
    await sweep(env, now);

    if (what === "host") {
        if (body.game !== GAME) return reply({ error: "game" }, 409);
        if (!versionAllowed(env, body.v)) return reply({ error: "version" }, 409);
        const open = await env.DB.prepare("SELECT COUNT(*) AS n FROM hl_rooms WHERE host_seen = ?").bind(ip).first();
        if (open && open.n >= (Number(env.ROOMS_PER_ADDRESS) || 4)) return reply({ error: "busy" }, 429);
        const key = randomKey();
        for (let tries = 0; tries < 8; tries++) {
            const code = randomCode();
            const r = await env.DB.prepare("INSERT OR IGNORE INTO hl_rooms (code, v, game, fp, host_key, host_name, host_addrs, host_seen, opts, created, touched) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .bind(code, body.v, GAME, cleanFp(body.fp), key, cleanName(body.name), JSON.stringify(cleanAddrs(body.addrs)), ip, JSON.stringify(cleanOpts(body.opts)), now, now).run();
            if (r.meta.changes === 1) return reply({ code, key, you: ip });
        }
        return reply({ error: "busy" }, 503);
    }

    if (what === "join") {
        const code = String(body.code ?? "").replace(/\D/g, "");
        const room = await env.DB.prepare("SELECT * FROM hl_rooms WHERE code = ?").bind(code).first();
        if (!room) return reply({ error: "no room" }, 404);
        if (body.game !== room.game) return reply({ error: "game" }, 409);
        if (room.v !== body.v) return reply({ error: "version" }, 409);
        if (room.fp !== cleanFp(body.fp)) return reply({ error: "different" }, 409);
        if (room.guest_key) return reply({ error: "full" }, 409);
        const key = randomKey();
        const r = await env.DB.prepare("UPDATE hl_rooms SET guest_key = ?, guest_name = ?, guest_addrs = ?, guest_seen = ?, touched = ? WHERE code = ? AND guest_key IS NULL")
            .bind(key, cleanName(body.name), JSON.stringify(cleanAddrs(body.addrs)), ip, now, code).run();
        if (r.meta.changes !== 1) return reply({ error: "full" }, 409);
        return reply({ key, you: ip, host: hostOf(room), opts: JSON.parse(room.opts) });
    }

    const who = await whoIs(env, body.code, body.key);
    if (!who) return reply({ error: "no room" }, 404);
    const code = who.room.code;

    if (what === "poll") {
        if (who.host) {
            await env.DB.prepare("UPDATE hl_rooms SET touched = ?, host_seen = ? WHERE code = ?").bind(now, ip, code).run();
            return reply({ guest: guestOf(who.room), opts: JSON.parse(who.room.opts) });
        }
        await env.DB.prepare("UPDATE hl_rooms SET touched = ?, guest_seen = ? WHERE code = ?").bind(now, ip, code).run();
        return reply({ host: hostOf(who.room), opts: JSON.parse(who.room.opts) });
    }

    if (what === "addrs") {
        const addrs = JSON.stringify(cleanAddrs(body.addrs));
        if (who.host) await env.DB.prepare("UPDATE hl_rooms SET host_addrs = ?, touched = ? WHERE code = ?").bind(addrs, now, code).run();
        else await env.DB.prepare("UPDATE hl_rooms SET guest_addrs = ?, touched = ? WHERE code = ?").bind(addrs, now, code).run();
        return reply({});
    }

    if (what === "relay") {
        if (!who.host) return reply({ error: "bad request" }, 403);
        const login = await relayLogin(env, now);
        return reply(login.server ? { turn: { server: login.server, username: login.username, credential: login.credential } } : { noRelay: login.why });
    }

    if (what === "leave") {
        if (who.host) await env.DB.prepare("DELETE FROM hl_rooms WHERE code = ?").bind(code).run();
        else await env.DB.prepare("UPDATE hl_rooms SET guest_key = NULL, guest_name = NULL, guest_addrs = NULL, guest_seen = NULL, touched = ? WHERE code = ?").bind(now, code).run();
        return reply({});
    }
    return reply({ error: "bad request" }, 404);
}

export default {
    async fetch(request, env) {
        try {
            return await handle(request, env, Math.floor(Date.now() / 1000));
        } catch (e) {
            console.error("hiddenleaf-online: " + (e && e.message));
            return reply({ error: "unavailable" }, 503);
        }
    },
};

// A room's relay (one Durable Object per room code). Its sockets hibernate between messages: a room costs nothing while
// nobody sends. Tags: "h" the host, "g" the guest. What one sends reaches the other as it is; a new socket of the same
// player replaces the old one (a phone that reconnected).
export class HiddenLeafRelay {
    constructor(state) {
        this.state = state;
    }
    async fetch(request) {
        const who = request.headers.get("x-hl-who") || "";
        if (who !== "h" && who !== "g") return new Response("bad request", { status: 400 });
        const pair = new WebSocketPair();
        const [client, server] = Object.values(pair);
        for (const old of this.state.getWebSockets(who)) {
            try { old.close(1000, "replaced"); } catch (e) {}
        }
        this.state.acceptWebSocket(server, [who]);
        return new Response(null, { status: 101, webSocket: client });
    }
    webSocketMessage(ws, message) {
        if (typeof message === "string" || message.byteLength < 1 || message.byteLength > MAX_RELAY_MESSAGE) return;
        const tag = this.state.getTags(ws)[0] || "";
        const other = tag === "h" ? "g" : tag === "g" ? "h" : "";
        if (!other) return;
        for (const peer of this.state.getWebSockets(other)) {
            try { peer.send(message); } catch (e) {}
        }
    }
    webSocketClose(ws, code) {
        try { ws.close(code === 1005 ? 1000 : code, "closed"); } catch (e) {}
    }
    webSocketError(ws) {
        try { ws.close(1011, "error"); } catch (e) {}
    }
}
