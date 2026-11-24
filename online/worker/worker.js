// Hidden Leaf Online: the rooms of Hidden Leaf (Naruto Shippuden: Ultimate Ninja 5, SLES-55605, game id "un5") over the
// internet. A Cloudflare Worker with a D1 database (schema.sql), made from Dragon Rage's Tag Team Online Worker
// (port/tagnet-worker there). It never carries the game itself: it gives a room a six-digit code, tells the two players
// each other's addresses (what each phone found out about itself: its own network and STUN), and passes the match's
// packets on through the room's own relay (a Durable Object, below) when the two phones cannot reach each other directly.
// The match goes between the phones over UDP (recomp/android/host/online/link.cpp), through the relay only when it must.
//
// Requests: POST /v1/<what> with a JSON body; answers are JSON (errors: {error: "..."} with a 4xx status).
//   /v1/hello  {v, game, fp?}                           -> {ok, v, game, colo, country, time, waiting?}   (the app's connection
//                                                          test; with `fp`, `waiting`: how many search a quick match it would meet)
//   /v1/host   {v, game, fp, name, addrs, opts}         -> {code, key, you}
//   /v1/join   {v, game, fp, code, name, addrs}         -> {key, you, host: {name, addrs, seen}, opts}
//                                                          ("no room", "version", "game", "different", "full")
//   /v1/poll   {code, key}                             -> host: {guest: {name, addrs, seen} | null, opts}
//                                                         guest: {host: {name, addrs, seen}, opts}   ("no room": it is gone)
//   /v1/addrs  {code, key, addrs}                      -> {}   (one's addresses again)
//   /v1/relay  {code, key}                             -> {turn: {server, username, credential}} | {noRelay: why}   (host only)
//   /v1/leave  {code, key}                             -> {}   (the host leaving closes the room; the guest leaving frees it)
//   /v1/quick  {v, game, fp, name, addrs, opts, who}   -> {role: "join", code, key, you, host: {name, addrs, seen}, opts}
//                                                       | {role: "host", code, key, you}   ("version", "game", "busy")
//       quick match: the caller is put in the room of whoever has waited longest with the same v, game and fp (as its
//       guest, as /v1/join makes one), or, if nobody waits, is given a room of its own that waits in the queue
//       (hl_queue): it then polls it as any host does (/v1/poll, /v1/addrs, /v1/leave, /v1/ws all the same). A waiting
//       room whose host has not polled for QUICK_IDLE seconds is given to nobody and goes at the next search (its host's
//       poll then gets "no room": it searches again). `who` is a random text the app made up, so that a room it left
//       behind (it searched, then searched again) is not the one it is given.
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
const QUICK_IDLE = 25;       // seconds a quick-match room waits in the queue without its host's poll
const TOUCH_EVERY = 12;      // a poll writes the room's time only this often (D1's free plan counts rows written)
const SWEEP_EVERY = 30;      // global sweep runs at most this often per isolate, and only on room creation
const RELAY_PER_DAY = 300;
const MAX_BODY = 4096;
const MAX_RELAY_MESSAGE = 1600;

let lastSweepAt = 0;
const rateBuckets = new Map(); // "bucket:ip" -> { win, n }

export function resetWorkerState() {
    lastSweepAt = 0;
    rateBuckets.clear();
}

function checkRate(ip, now, bucket, limitPerMin) {
    if (!ip || limitPerMin <= 0) return true;
    const win = Math.floor(now / 60);
    const key = bucket + ":" + ip;
    const cur = rateBuckets.get(key);
    if (!cur || cur.win !== win) {
        if (rateBuckets.size > 4096) rateBuckets.clear();
        rateBuckets.set(key, { win, n: 1 });
        return true;
    }
    if (cur.n >= limitPerMin) return false;
    cur.n++;
    return true;
}

function cleanInputMode(s) {
    return s === "touch" || s === "gamepad" ? s : "any";
}

function cleanPlayerId(s) {
    return typeof s === "string" ? s.replace(/[^0-9a-zA-Z_-]/g, "").slice(0, 48) : "";
}

async function sha256Hex(text) {
    const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(String(text)));
    return [...new Uint8Array(buf)].map((x) => x.toString(16).padStart(2, "0")).join("");
}

function cleanCharId(s) {
    return typeof s === "string" ? s.toLowerCase().replace(/[^a-z0-9_-]/g, "").slice(0, 24) || "naruto" : "naruto";
}

function charLevelFromXp(xp) {
    return Math.max(1, Math.min(99, 1 + Math.floor(Math.max(0, Number(xp) || 0) / 200)));
}

export function ninjaTier(points) {
    if (points >= 2000) return "Kage";
    if (points >= 1700) return "Sannin";
    if (points >= 1500) return "ANBU";
    if (points >= 1300) return "Jonin";
    if (points >= 1100) return "Chunin";
    return "Genin";
}

// Elo/RP points delta: K=32, bounded [8..48], +4 bonus for 3+ win streak.
export function eloDelta(winnerPts, loserPts, winnerStreak = 0) {
    const expected = 1 / (1 + Math.pow(10, (loserPts - winnerPts) / 400));
    const base = Math.round(32 * (1 - expected));
    const gain = Math.max(8, Math.min(48, base)) + (winnerStreak >= 2 ? 4 : 0);
    const loss = Math.max(8, Math.min(40, base));
    return { gain, loss };
}

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
        if (typeof o.spectators === "boolean") out.spectators = o.spectators;
        if (typeof o.ranked === "boolean") out.ranked = o.ranked;
    }
    return out;
}

export function versionAllowed(env, v) {
    if (!Number.isInteger(v)) return false;
    const list = String(env.ALLOWED_VERSIONS ?? "").split(",").map((x) => Number.parseInt(x, 10)).filter(Number.isInteger);
    return list.length === 0 || list.includes(v);
}

// Whether an error thrown by D1 or a Durable Object says a free daily limit is used up (rows read or written, requests).
export function quotaError(message) {
    return /exceed|quota|daily limit|rows (read|written)/i.test(String(message ?? ""));
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

// A room of [ip]'s with the body's player as its host: {code, key, token, input_mode}, or null if no free code was found.
async function makeRoom(env, body, ip, now) {
    const key = randomKey();
    const token = randomKey();
    const inputMode = cleanInputMode(body.input_mode);
    for (let tries = 0; tries < 8; tries++) {
        const code = randomCode();
        const r = await env.DB.prepare("INSERT OR IGNORE INTO hl_rooms (code, v, game, fp, input_mode, session_secret, host_key, host_name, host_addrs, host_seen, opts, created, touched) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
            .bind(code, body.v, GAME, cleanFp(body.fp), inputMode, token, key, cleanName(body.name), JSON.stringify(cleanAddrs(body.addrs)), ip, JSON.stringify(cleanOpts(body.opts)), now, now).run();
        if (r.meta.changes === 1) return { code, key, token, input_mode: inputMode };
    }
    return null;
}

async function tooManyRooms(env, ip, now) {
    const open = await env.DB.prepare("SELECT COUNT(*) AS n FROM hl_rooms WHERE host_seen = ? AND touched >= ?").bind(ip, now - ROOM_IDLE).first();
    return open && open.n >= (Number(env.ROOMS_PER_ADDRESS) || 4);
}

// The quick-match rooms that wait for a guest with one v, game, fp and input_mode (bound in that order, then the oldest poll time
// taken: now - QUICK_IDLE). The longest waiting comes first by the queue's rowid.
const WAITING = "FROM hl_queue q JOIN hl_rooms r ON r.code = q.code WHERE q.v = ? AND q.game = ? AND q.fp = ? AND q.input_mode = ? AND r.guest_key IS NULL AND r.touched >= ?";

// Makes the caller [room]'s guest, as /v1/join does; null if someone else got there first.
async function takeRoom(env, room, body, ip, now) {
    const key = randomKey();
    const r = await env.DB.prepare("UPDATE hl_rooms SET guest_key = ?, guest_name = ?, guest_addrs = ?, guest_seen = ?, touched = ? WHERE code = ? AND guest_key IS NULL")
        .bind(key, cleanName(body.name), JSON.stringify(cleanAddrs(body.addrs)), ip, now, room.code).run();
    return r.meta.changes === 1 ? { key, token: room.session_secret || "", input_mode: room.input_mode || "any", you: ip, host: hostOf(room), opts: JSON.parse(room.opts) } : null;
}

async function sweep(env, now) {
    if (now - lastSweepAt < SWEEP_EVERY && now >= lastSweepAt) return;
    lastSweepAt = now;
    await env.DB.prepare("DELETE FROM hl_rooms WHERE touched < ?").bind(now - ROOM_IDLE).run();
}

// Who asks: the room's host or its guest, by the key each was given. Lazily rejects expired rooms without writing to D1.
async function whoIs(env, code, key, now) {
    if (typeof code !== "string" || typeof key !== "string" || key.length < 16) return null;
    const room = await env.DB.prepare("SELECT * FROM hl_rooms WHERE code = ?").bind(code).first();
    if (!room) return null;
    if (now - room.touched >= ROOM_IDLE) return null;
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
    if (request.method === "GET" && url.pathname === "/v1/leaderboard") {
        const limit = Math.max(1, Math.min(100, Number.parseInt(url.searchParams.get("limit") || "100", 10) || 100));
        const mode = url.searchParams.get("mode") || "all";
        let rows;
        if (mode === "touch" || mode === "gamepad") {
            rows = (await env.DB.prepare("SELECT player_id, name, points, wins, losses, streak, input_mode, fav_char, fav_char_lvl FROM hl_players WHERE input_mode = ? ORDER BY points DESC, wins DESC LIMIT ?")
                .bind(mode, limit).all()).results;
        } else {
            rows = (await env.DB.prepare("SELECT player_id, name, points, wins, losses, streak, input_mode, fav_char, fav_char_lvl FROM hl_players ORDER BY points DESC, wins DESC LIMIT ?")
                .bind(limit).all()).results;
        }
        const entries = rows.map((r, i) => ({
            rank: i + 1,
            player_id: r.player_id,
            name: r.name,
            points: r.points,
            wins: r.wins,
            losses: r.losses,
            streak: r.streak,
            input_mode: r.input_mode,
            fav_char: r.fav_char || "naruto",
            fav_char_lvl: r.fav_char_lvl || 1,
            tier: ninjaTier(r.points),
        }));
        return new Response(JSON.stringify({ entries, updated: now }), {
            status: 200,
            headers: { "content-type": "application/json", "cache-control": "public, max-age=60" },
        });
    }
    if (request.method === "GET" && url.pathname === "/v1/ws") {
        if (request.headers.get("upgrade") !== "websocket") return reply({ error: "bad request" }, 426);
        if (!env.RELAY) return reply({ error: "no relay" }, 503);
        const roleParam = url.searchParams.get("role") || "";
        const codeParam = url.searchParams.get("code") || "";
        let tag = "";
        let roomCode = "";
        if (roleParam === "spectator") {
            const room = await env.DB.prepare("SELECT * FROM hl_rooms WHERE code = ?").bind(codeParam).first();
            if (!room || now - room.touched >= ROOM_IDLE) return reply({ error: "no room" }, 404);
            const opts = JSON.parse(room.opts || "{}");
            if (opts.spectators === false) return reply({ error: "no spectators" }, 403);
            tag = "s";
            roomCode = room.code;
        } else {
            const who = await whoIs(env, codeParam, url.searchParams.get("key"), now);
            if (!who) return reply({ error: "no room" }, 404);
            if (now - who.room.touched >= TOUCH_EVERY) {
                await env.DB.prepare("UPDATE hl_rooms SET touched = ? WHERE code = ?").bind(now, who.room.code).run();
            }
            tag = who.host ? "h" : "g";
            roomCode = who.room.code;
        }
        const headers = new Headers(request.headers);
        headers.set("x-hl-who", tag);
        // The relay is made near whoever connects first (left to Cloudflare it may sit on another continent: 100-150 ms more
        // each way between two players in Brazil, as Dragon Rage measured).
        const hints = { SA: "sam", NA: "enam", EU: "weur", AS: "apac", OC: "oc", AF: "afr" };
        const hint = hints[(request.cf && request.cf.continent) || ""] || "sam";
        const stub = env.RELAY.get(env.RELAY.idFromName(roomCode), { locationHint: hint });
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
        const ok = versionAllowed(env, body.v) && body.game === GAME;
        const out = { ok, v: body.v ?? null, game: GAME, colo: cf.colo ?? "", country: cf.country ?? "", time: now };
        if (ok && typeof body.fp === "string") { // (a read only: nothing written)
            const inputMode = cleanInputMode(body.input_mode);
            const n = await env.DB.prepare(`SELECT COUNT(*) AS n ${WAITING}`).bind(body.v, GAME, cleanFp(body.fp), inputMode, now - QUICK_IDLE).first();
            out.waiting = n ? n.n : 0;
        }
        return reply(out);
    }

    if (what === "host") {
        if (body.game !== GAME) return reply({ error: "game" }, 409);
        if (!versionAllowed(env, body.v)) return reply({ error: "version" }, 409);
        if (!checkRate(ip, now, "host", Number(env.RATE_HOST_PER_MIN) || 12)) return reply({ error: "busy" }, 429);
        await sweep(env, now);
        if (await tooManyRooms(env, ip, now)) return reply({ error: "busy" }, 429);
        const made = await makeRoom(env, body, ip, now);
        return made ? reply({ ...made, you: ip }) : reply({ error: "busy" }, 503);
    }

    if (what === "quick") {
        if (body.game !== GAME) return reply({ error: "game" }, 409);
        if (!versionAllowed(env, body.v)) return reply({ error: "version" }, 409);
        const who = typeof body.who === "string" ? body.who.replace(/[^0-9A-Za-z]/g, "").slice(0, 32) : "";
        if (who.length < 8) return reply({ error: "bad request" }, 400);
        if (!checkRate(ip, now, "quick", Number(env.RATE_QUICK_PER_MIN) || 20)) return reply({ error: "busy" }, 429);
        await sweep(env, now);
        const fp = cleanFp(body.fp);
        const inputMode = cleanInputMode(body.input_mode);
        // Out of the queue: rooms whose host stopped polling and rooms this same phone left waiting (nobody in them yet);
        // then the queue's entries whose room is gone (left, swept, or just deleted here).
        await env.DB.prepare("DELETE FROM hl_rooms WHERE guest_key IS NULL AND code IN (SELECT code FROM hl_queue) AND (touched < ? OR code IN (SELECT code FROM hl_queue WHERE who = ?))")
            .bind(now - QUICK_IDLE, who).run();
        await env.DB.prepare("DELETE FROM hl_queue WHERE code NOT IN (SELECT code FROM hl_rooms)").run();
        for (let attempt = 0; attempt < 3; attempt++) {
            const waiting = await env.DB.prepare(`SELECT r.* ${WAITING} AND q.who <> ? ORDER BY q.rowid LIMIT 1`)
                .bind(body.v, GAME, fp, inputMode, now - QUICK_IDLE, who).first();
            if (waiting) {
                const joined = await takeRoom(env, waiting, body, ip, now);
                if (joined) {
                    await env.DB.prepare("DELETE FROM hl_queue WHERE code = ?").bind(waiting.code).run();
                    return reply({ role: "join", code: waiting.code, ...joined });
                }
                continue; // someone else took that one this moment: the next
            }
            if (await tooManyRooms(env, ip, now)) return reply({ error: "busy" }, 429);
            const mine = await makeRoom(env, body, ip, now);
            if (!mine) return reply({ error: "busy" }, 503);
            await env.DB.prepare("INSERT INTO hl_queue (code, v, game, fp, input_mode, who, made) VALUES (?, ?, ?, ?, ?, ?, ?)")
                .bind(mine.code, body.v, GAME, fp, inputMode, who, now).run();
            // Two who search in the same moment both find nobody and both make a room. The one whose room came second
            // sees the first one's here, gives its own up (unless someone is in it already) and takes the first.
            const earlier = await env.DB.prepare(`SELECT q.code ${WAITING} AND q.who <> ? AND q.rowid < (SELECT rowid FROM hl_queue WHERE code = ?) LIMIT 1`)
                .bind(body.v, GAME, fp, inputMode, now - QUICK_IDLE, who, mine.code).first();
            if (earlier) {
                const gone = await env.DB.prepare("DELETE FROM hl_rooms WHERE code = ? AND guest_key IS NULL").bind(mine.code).run();
                if (gone.meta.changes === 1) {
                    await env.DB.prepare("DELETE FROM hl_queue WHERE code = ?").bind(mine.code).run();
                    continue;
                }
            }
            return reply({ role: "host", ...mine, you: ip });
        }
        return reply({ error: "busy" }, 503);
    }

    if (what === "join") {
        if (!checkRate(ip, now, "join", Number(env.RATE_JOIN_PER_MIN) || 24)) return reply({ error: "busy" }, 429);
        const code = String(body.code ?? "").replace(/\D/g, "");
        const room = await env.DB.prepare("SELECT * FROM hl_rooms WHERE code = ?").bind(code).first();
        if (!room || now - room.touched >= ROOM_IDLE) return reply({ error: "no room" }, 404);
        if (body.game !== room.game) return reply({ error: "game" }, 409);
        if (room.v !== body.v) return reply({ error: "version" }, 409);
        if (room.fp !== cleanFp(body.fp)) return reply({ error: "different" }, 409);
        const reqMode = cleanInputMode(body.input_mode);
        const roomMode = room.input_mode || "any";
        if (roomMode !== "any" && reqMode !== "any" && roomMode !== reqMode) return reply({ error: "input_mode" }, 409);
        if (room.guest_key) return reply({ error: "full" }, 409);
        const key = randomKey();
        const r = await env.DB.prepare("UPDATE hl_rooms SET guest_key = ?, guest_name = ?, guest_addrs = ?, guest_seen = ?, touched = ? WHERE code = ? AND guest_key IS NULL")
            .bind(key, cleanName(body.name), JSON.stringify(cleanAddrs(body.addrs)), ip, now, code).run();
        if (r.meta.changes !== 1) return reply({ error: "full" }, 409);
        return reply({ key, token: room.session_secret || "", input_mode: roomMode, you: ip, host: hostOf(room), opts: JSON.parse(room.opts) });
    }

    // ---- Ranked profile & bilateral result reporting
    if (what === "profile") {
        const pid = cleanPlayerId(body.player_id);
        const secret = typeof body.secret === "string" ? body.secret : "";
        if (pid.length < 8 || secret.length < 16) return reply({ error: "bad request" }, 400);
        const name = cleanName(body.name) || "Ninja";
        const mode = cleanInputMode(body.input_mode);
        const favChar = body.fav_char ? cleanCharId(body.fav_char) : null;
        const hash = await sha256Hex(secret);
        const existing = await env.DB.prepare("SELECT * FROM hl_players WHERE player_id = ?").bind(pid).first();
        if (existing) {
            if (existing.auth_hash !== hash) return reply({ error: "unauthorized" }, 403);
            const nextChar = favChar || existing.fav_char || "naruto";
            await env.DB.prepare("UPDATE hl_players SET name = ?, input_mode = ?, fav_char = ?, updated = ? WHERE player_id = ?")
                .bind(name, mode, nextChar, now, pid).run();
            const rankRow = await env.DB.prepare("SELECT COUNT(*) + 1 AS rk FROM hl_players WHERE points > ? OR (points = ? AND wins > ?)")
                .bind(existing.points, existing.points, existing.wins).first();
            return reply({
                player_id: pid, name, points: existing.points, wins: existing.wins, losses: existing.losses,
                streak: existing.streak, input_mode: mode, fav_char: nextChar,
                fav_char_lvl: existing.fav_char_lvl || 1, fav_char_xp: existing.fav_char_xp || 0,
                rank: rankRow ? rankRow.rk : 1, tier: ninjaTier(existing.points),
            });
        }
        if (!checkRate(ip, now, "profile", 10)) return reply({ error: "busy" }, 429);
        const initChar = favChar || "naruto";
        await env.DB.prepare("INSERT INTO hl_players (player_id, auth_hash, name, points, wins, losses, streak, input_mode, fav_char, fav_char_lvl, fav_char_xp, updated) VALUES (?, ?, ?, 1000, 0, 0, 0, ?, ?, 1, 0, ?)")
            .bind(pid, hash, name, mode, initChar, now).run();
        const rankRow = await env.DB.prepare("SELECT COUNT(*) + 1 AS rk FROM hl_players WHERE points > 1000 OR (points = 1000 AND wins > 0)").first();
        return reply({
            player_id: pid, name, points: 1000, wins: 0, losses: 0, streak: 0, input_mode: mode,
            fav_char: initChar, fav_char_lvl: 1, fav_char_xp: 0, rank: rankRow ? rankRow.rk : 1, tier: ninjaTier(1000),
        });
    }

    if (what === "ranked/report") {
        const pid = cleanPlayerId(body.player_id);
        const oppId = cleanPlayerId(body.opponent_id);
        const secret = typeof body.secret === "string" ? body.secret : "";
        const matchId = cleanPlayerId(body.match_id);
        const claim = body.winner === "p1" || body.winner === "p2" || body.winner === "ragequit" ? body.winner : "";
        const role = body.role === "p1" || body.role === "p2" ? body.role : "";
        if (!pid || !oppId || pid === oppId || secret.length < 16 || matchId.length < 6 || !claim || !role) {
            return reply({ error: "bad request" }, 400);
        }
        const player = await env.DB.prepare("SELECT * FROM hl_players WHERE player_id = ?").bind(pid).first();
        if (!player || player.auth_hash !== (await sha256Hex(secret))) return reply({ error: "unauthorized" }, 403);
        const p1Id = role === "p1" ? pid : oppId;
        const p2Id = role === "p1" ? oppId : pid;
        let m = await env.DB.prepare("SELECT * FROM hl_matches WHERE match_id = ?").bind(matchId).first();
        if (!m) {
            await env.DB.prepare("INSERT OR IGNORE INTO hl_matches (match_id, p1_id, p2_id, p1_claim, p2_claim, created) VALUES (?, ?, ?, ?, ?, ?)")
                .bind(matchId, p1Id, p2Id, role === "p1" ? claim : null, role === "p2" ? claim : null, now).run();
            m = await env.DB.prepare("SELECT * FROM hl_matches WHERE match_id = ?").bind(matchId).first();
        } else {
            if (m.resolved !== 0) return reply({ resolved: m.resolved === 1, winner_id: m.winner_id, p1_delta: m.p1_delta, p2_delta: m.p2_delta });
            if (role === "p1" && m.p1_id === pid) {
                await env.DB.prepare("UPDATE hl_matches SET p1_claim = ? WHERE match_id = ?").bind(claim, matchId).run();
                m.p1_claim = claim;
            } else if (role === "p2" && m.p2_id === pid) {
                await env.DB.prepare("UPDATE hl_matches SET p2_claim = ? WHERE match_id = ?").bind(claim, matchId).run();
                m.p2_claim = claim;
            } else {
                return reply({ error: "unauthorized" }, 403);
            }
        }
        // Resolve when both sides agree, or when one side reports peer ragequit (the reporter wins)
        let winnerRole = "";
        if (m.p1_claim && m.p2_claim) {
            if (m.p1_claim === m.p2_claim && (m.p1_claim === "p1" || m.p1_claim === "p2")) winnerRole = m.p1_claim;
            else {
                await env.DB.prepare("UPDATE hl_matches SET resolved = -1 WHERE match_id = ?").bind(matchId).run();
                return reply({ resolved: false, disputed: true });
            }
        } else if (claim === "ragequit") {
            winnerRole = role; // reporter stayed connected
        } else {
            return reply({ resolved: false, waiting: true });
        }
        const p1 = await env.DB.prepare("SELECT * FROM hl_players WHERE player_id = ?").bind(m.p1_id).first();
        const p2 = await env.DB.prepare("SELECT * FROM hl_players WHERE player_id = ?").bind(m.p2_id).first();
        if (!p1 || !p2) return reply({ error: "no player" }, 404);
        const p1Won = winnerRole === "p1";
        const winner = p1Won ? p1 : p2;
        const loser = p1Won ? p2 : p1;
        const { gain, loss } = eloDelta(winner.points, loser.points, winner.streak);
        const wPts = winner.points + gain;
        const lPts = Math.max(0, loser.points - loss);
        const p1Delta = p1Won ? gain : -loss;
        const p2Delta = p1Won ? -loss : gain;
        const wXp = (winner.fav_char_xp || 0) + 120;
        const lXp = (loser.fav_char_xp || 0) + 45;
        await env.DB.prepare("UPDATE hl_players SET points = ?, wins = wins + 1, streak = streak + 1, fav_char_xp = ?, fav_char_lvl = ?, updated = ? WHERE player_id = ?")
            .bind(wPts, wXp, charLevelFromXp(wXp), now, winner.player_id).run();
        await env.DB.prepare("UPDATE hl_players SET points = ?, losses = losses + 1, streak = 0, fav_char_xp = ?, fav_char_lvl = ?, updated = ? WHERE player_id = ?")
            .bind(lPts, lXp, charLevelFromXp(lXp), now, loser.player_id).run();
        await env.DB.prepare("UPDATE hl_matches SET winner_id = ?, p1_delta = ?, p2_delta = ?, resolved = 1 WHERE match_id = ?")
            .bind(winner.player_id, p1Delta, p2Delta, matchId).run();
        return reply({
            resolved: true,
            winner_id: winner.player_id,
            p1_points: p1Won ? wPts : lPts,
            p2_points: p1Won ? lPts : wPts,
            p1_delta: p1Delta,
            p2_delta: p2Delta,
        });
    }

    // ---- Custom Lobbies (2-8 players, King-of-the-Hill queue + spectators)
    if (what === "lobby/create") {
        if (body.game !== GAME) return reply({ error: "game" }, 409);
        if (!versionAllowed(env, body.v)) return reply({ error: "version" }, 409);
        if (!checkRate(ip, now, "lobby", 10)) return reply({ error: "busy" }, 429);
        const pid = cleanPlayerId(body.player_id) || randomKey().slice(0, 16);
        const name = cleanName(body.name) || "Host";
        const maxPlayers = Math.max(2, Math.min(8, Number.parseInt(body.max_players ?? 4, 10) || 4));
        const allowSpec = body.allow_spectators === false ? 0 : 1;
        const inputMode = cleanInputMode(body.input_mode);
        const members = [{ id: pid, name, role: "p1", joined: now }];
        for (let tries = 0; tries < 8; tries++) {
            const code = randomCode();
            const r = await env.DB.prepare("INSERT OR IGNORE INTO hl_lobbies (code, v, game, fp, host_id, input_mode, max_players, allow_spectators, members, created, touched) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .bind(code, body.v, GAME, cleanFp(body.fp), pid, inputMode, maxPlayers, allowSpec, JSON.stringify(members), now, now).run();
            if (r.meta.changes === 1) {
                return reply({ code, player_id: pid, max_players: maxPlayers, allow_spectators: !!allowSpec, input_mode: inputMode, members });
            }
        }
        return reply({ error: "busy" }, 503);
    }

    if (what === "lobby/join") {
        const code = String(body.code ?? "").replace(/\D/g, "");
        const lobby = await env.DB.prepare("SELECT * FROM hl_lobbies WHERE code = ?").bind(code).first();
        if (!lobby || now - lobby.touched >= ROOM_IDLE * 2) return reply({ error: "no room" }, 404);
        if (body.game !== lobby.game) return reply({ error: "game" }, 409);
        if (lobby.v !== body.v) return reply({ error: "version" }, 409);
        if (lobby.fp !== cleanFp(body.fp)) return reply({ error: "different" }, 409);
        const reqMode = cleanInputMode(body.input_mode);
        if (lobby.input_mode !== "any" && reqMode !== "any" && lobby.input_mode !== reqMode) return reply({ error: "input_mode" }, 409);
        const members = JSON.parse(lobby.members || "[]");
        const pid = cleanPlayerId(body.player_id) || randomKey().slice(0, 16);
        const name = cleanName(body.name) || "Ninja";
        const wantSpec = body.spectator === true;
        if (wantSpec && !lobby.allow_spectators) return reply({ error: "no spectators" }, 403);
        const existing = members.find((m) => m.id === pid);
        if (!existing) {
            if (members.length >= lobby.max_players) return reply({ error: "full" }, 409);
            const hasP2 = members.some((m) => m.role === "p2");
            const role = wantSpec ? "spectator" : (!hasP2 ? "p2" : "queue");
            members.push({ id: pid, name, role, joined: now });
            await env.DB.prepare("UPDATE hl_lobbies SET members = ?, touched = ? WHERE code = ?")
                .bind(JSON.stringify(members), now, code).run();
        }
        return reply({
            code, player_id: pid, host_id: lobby.host_id, max_players: lobby.max_players,
            allow_spectators: !!lobby.allow_spectators, input_mode: lobby.input_mode,
            active_room: lobby.active_room || null, members,
        });
    }

    if (what === "lobby/state") {
        const code = String(body.code ?? "").replace(/\D/g, "");
        const lobby = await env.DB.prepare("SELECT * FROM hl_lobbies WHERE code = ?").bind(code).first();
        if (!lobby || now - lobby.touched >= ROOM_IDLE * 2) return reply({ error: "no room" }, 404);
        if (body.active_room && body.player_id === lobby.host_id) {
            const ar = String(body.active_room).replace(/\D/g, "").slice(0, 6);
            await env.DB.prepare("UPDATE hl_lobbies SET active_room = ?, touched = ? WHERE code = ?").bind(ar, now, code).run();
            lobby.active_room = ar;
        } else if (now - lobby.touched >= TOUCH_EVERY) {
            await env.DB.prepare("UPDATE hl_lobbies SET touched = ? WHERE code = ?").bind(now, code).run();
        }
        return reply({
            code, host_id: lobby.host_id, max_players: lobby.max_players,
            allow_spectators: !!lobby.allow_spectators, input_mode: lobby.input_mode,
            active_room: lobby.active_room || null, members: JSON.parse(lobby.members || "[]"),
        });
    }

    if (what === "lobby/rotate") {
        // King-of-the-Hill rotation: winner stays as P1, loser moves to back of queue, next queued player becomes P2.
        const code = String(body.code ?? "").replace(/\D/g, "");
        const lobby = await env.DB.prepare("SELECT * FROM hl_lobbies WHERE code = ?").bind(code).first();
        if (!lobby || lobby.host_id !== cleanPlayerId(body.player_id)) return reply({ error: "unauthorized" }, 403);
        const members = JSON.parse(lobby.members || "[]");
        const p1 = members.find((m) => m.role === "p1");
        const p2 = members.find((m) => m.role === "p2");
        const queue = members.filter((m) => m.role === "queue");
        const winner = body.winner === "p2" ? p2 : p1;
        const loser = body.winner === "p2" ? p1 : p2;
        if (winner && loser && queue.length > 0) {
            const nextChallenger = queue[0];
            winner.role = "p1";
            nextChallenger.role = "p2";
            loser.role = "queue";
            // Reorder so loser goes to the back of the queue
            const rest = members.filter((m) => m !== loser);
            rest.push(loser);
            await env.DB.prepare("UPDATE hl_lobbies SET members = ?, active_room = NULL, touched = ? WHERE code = ?")
                .bind(JSON.stringify(rest), now, code).run();
            return reply({ code, members: rest });
        }
        return reply({ code, members });
    }

    if (what === "lobby/leave") {
        const code = String(body.code ?? "").replace(/\D/g, "");
        const pid = cleanPlayerId(body.player_id);
        const lobby = await env.DB.prepare("SELECT * FROM hl_lobbies WHERE code = ?").bind(code).first();
        if (!lobby) return reply({});
        if (lobby.host_id === pid) {
            await env.DB.prepare("DELETE FROM hl_lobbies WHERE code = ?").bind(code).run();
        } else {
            const members = JSON.parse(lobby.members || "[]").filter((m) => m.id !== pid);
            await env.DB.prepare("UPDATE hl_lobbies SET members = ?, touched = ? WHERE code = ?")
                .bind(JSON.stringify(members), now, code).run();
        }
        return reply({});
    }

    const who = await whoIs(env, body.code, body.key, now);
    if (!who) return reply({ error: "no room" }, 404);
    const code = who.room.code;

    if (what === "poll") {
        // (the room's time is written once in TOUCH_EVERY seconds, or when the player's address changed)
        const stale = now - who.room.touched >= TOUCH_EVERY;
        if (who.host) {
            if (stale || who.room.host_seen !== ip) await env.DB.prepare("UPDATE hl_rooms SET touched = ?, host_seen = ? WHERE code = ?").bind(now, ip, code).run();
            return reply({ guest: guestOf(who.room), opts: JSON.parse(who.room.opts), token: who.room.session_secret || "", input_mode: who.room.input_mode || "any" });
        }
        if (stale || who.room.guest_seen !== ip) await env.DB.prepare("UPDATE hl_rooms SET touched = ?, guest_seen = ? WHERE code = ?").bind(now, ip, code).run();
        return reply({ host: hostOf(who.room), opts: JSON.parse(who.room.opts), token: who.room.session_secret || "", input_mode: who.room.input_mode || "any" });
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
        if (who.host) {
            await env.DB.prepare("DELETE FROM hl_rooms WHERE code = ?").bind(code).run();
            await env.DB.prepare("DELETE FROM hl_queue WHERE code = ?").bind(code).run();
        } else {
            await env.DB.prepare("UPDATE hl_rooms SET guest_key = NULL, guest_name = NULL, guest_addrs = NULL, guest_seen = NULL, touched = ? WHERE code = ?").bind(now, code).run();
        }
        return reply({});
    }
    return reply({ error: "bad request" }, 404);
}

export default {
    async fetch(request, env) {
        try {
            return await handle(request, env, Math.floor(Date.now() / 1000));
        } catch (e) {
            const why = String((e && e.message) || "");
            console.error("hiddenleaf-online: " + why);
            // D1's or the Durable Objects' free daily limits used up: the app says so (they come back at 00:00 UTC)
            if (quotaError(why)) return reply({ error: "quota" }, 429);
            return reply({ error: "unavailable" }, 503);
        }
    },
};

// A room's relay (one Durable Object per room code). Its sockets hibernate between messages: a room costs nothing while
// nobody sends. Tags: "h" the host, "g" the guest, "s" read-only spectators (up to 4). What h/g sends reaches the opponent
// unchanged, and a tagged copy ([0x53, 'h'|'g', ...payload]) is fanned out to live spectators (~300 B/s).
export class HiddenLeafRelay {
    constructor(state) {
        this.state = state;
    }
    async fetch(request) {
        const who = request.headers.get("x-hl-who") || "";
        if (who !== "h" && who !== "g" && who !== "s") return new Response("bad request", { status: 400 });
        if (who === "s" && this.state.getWebSockets("s").length >= 4) {
            return new Response("spectators full", { status: 429 });
        }
        const pair = new WebSocketPair();
        const [client, server] = Object.values(pair);
        if (who === "h" || who === "g") {
            for (const old of this.state.getWebSockets(who)) {
                try { old.close(1000, "replaced"); } catch (e) {}
            }
        }
        this.state.acceptWebSocket(server, [who]);
        return new Response(null, { status: 101, webSocket: client });
    }
    webSocketMessage(ws, message) {
        if (typeof message === "string" || message.byteLength < 1 || message.byteLength > MAX_RELAY_MESSAGE) return;
        const tag = this.state.getTags(ws)[0] || "";
        if (tag !== "h" && tag !== "g") return; // spectators are strictly read-only
        const other = tag === "h" ? "g" : "h";
        for (const peer of this.state.getWebSockets(other)) {
            try { peer.send(message); } catch (e) {}
        }
        const spectators = this.state.getWebSockets("s");
        if (spectators.length > 0) {
            const raw = new Uint8Array(message);
            const wrapped = new Uint8Array(2 + raw.byteLength);
            wrapped[0] = 0x53; // 'S' spectator frame marker
            wrapped[1] = tag === "h" ? 1 : 2;
            wrapped.set(raw, 2);
            for (const sp of spectators) {
                try { sp.send(wrapped.buffer); } catch (e) {}
            }
        }
    }
    webSocketClose(ws, code) {
        try { ws.close(code === 1005 ? 1000 : code, "closed"); } catch (e) {}
    }
    webSocketError(ws) {
        try { ws.close(1011, "error"); } catch (e) {}
    }
}
