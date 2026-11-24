// The Worker against a real SQLite database standing in for D1 (Node 22+: node:sqlite). Run: node test.mjs
// Nothing is sent anywhere: the Durable Object and the TURN service are stand-ins.
import { DatabaseSync } from "node:sqlite";
import fs from "node:fs";
import assert from "node:assert/strict";
import worker, { handle, HiddenLeafRelay, GAME, quotaError } from "./worker.js";

let written = 0;          // rows D1 would count as written (the free plan's daily limit)
let beforeQueue = null;   // see run() below
const db = new DatabaseSync(":memory:");
db.exec(fs.readFileSync(new URL("./schema.sql", import.meta.url), "utf8"));
const D1 = {
    prepare(sql) {
        return {
            args: [],
            bind(...a) { this.args = a; return this; },
            async first() { return db.prepare(sql).get(...this.args) ?? null; },
            async all() { return { results: db.prepare(sql).all(...this.args) }; },
            async run() {
                // (a test may step in just before a quick-match room enters the queue: another player searching at that moment)
                if (beforeQueue && sql.startsWith("INSERT INTO hl_queue")) { const h = beforeQueue; beforeQueue = null; await h(); }
                const r = db.prepare(sql).run(...this.args);
                written += Number(r.changes);
                return { meta: { changes: r.changes } };
            },
        };
    },
    async batch(list) { return Promise.all(list.map((s) => s.run())); },
};

// The relay's binding: records what the Worker passed on (the room and who), answers 101 as the real one does.
const relayCalls = [];
const RELAY = {
    idFromName(name) { return { name }; },
    get(id, opts) {
        return { async fetch(req) { relayCalls.push({ room: id.name, who: req.headers.get("x-hl-who"), hint: opts?.locationHint }); return new Response(null, { status: 200 }); } };
    },
};

let now = 1_800_000_000;
const env = { DB: D1, RELAY, ALLOWED_VERSIONS: "1" };
async function call(what, body, ip = "203.0.113.1") {
    const r = await handle(new Request("https://t/v1/" + what, { method: "POST", body: JSON.stringify(body), headers: { "cf-connecting-ip": ip } }), env, now);
    return { status: r.status, ...(await r.json()) };
}
const fp = "app42+mods:none";
const v = 1;

// the connection test
let hello = await call("hello", { v, game: GAME });
assert.equal(hello.ok, true);
assert.equal(hello.time, now);
assert.equal((await call("hello", { v: 2, game: GAME })).ok, false);
assert.equal((await call("hello", { v, game: "bt2" })).ok, false);

// a room: wrong game or version refused; the host's addresses cleaned, its options kept
assert.equal((await call("host", { v, game: "bt2", name: "A", addrs: [] })).error, "game");
assert.equal((await call("host", { v: 7, game: GAME, name: "A", addrs: [] })).error, "version");
const h = await call("host", { v, game: GAME, fp, name: "Naruto\u0001\"", addrs: ["192.168.0.2:7001", "198.51.100.7:40000", "bad addr"], opts: { delay: 2, relay: false } });
assert.equal(h.status, 200);
assert.match(h.code, /^\d{6}$/);
assert.equal(h.you, "203.0.113.1");

// the host waits: nobody yet
let p = await call("poll", { code: h.code, key: h.key });
assert.equal(p.guest, null);
assert.deepEqual(p.opts, { delay: 2, relay: false });

// joining: no room, another build, then the guest
assert.equal((await call("join", { v, game: GAME, fp, code: "000000", name: "X", addrs: [] })).error, "no room");
assert.equal((await call("join", { v, game: GAME, fp: "app41+mods:none", code: h.code, name: "X", addrs: [] })).error, "different");
assert.equal((await call("join", { v: 2, game: GAME, fp, code: h.code, name: "X", addrs: [] })).error, "version");
const g = await call("join", { v, game: GAME, fp, code: " " + h.code.slice(0, 3) + "-" + h.code.slice(3), name: "Sasuke", addrs: ["10.0.0.3:5000"] }, "198.51.100.9");
assert.equal(g.status, 200);
assert.equal(g.host.name, "Naruto");
assert.deepEqual(g.host.addrs, ["192.168.0.2:7001", "198.51.100.7:40000"]);
assert.equal(g.host.seen, "203.0.113.1");
assert.deepEqual(g.opts, { delay: 2, relay: false });
// a room takes one guest
assert.equal((await call("join", { v, game: GAME, fp, code: h.code, name: "Third", addrs: [] }, "198.51.100.10")).error, "full");

// each sees the other
p = await call("poll", { code: h.code, key: h.key });
assert.equal(p.guest.name, "Sasuke");
assert.deepEqual(p.guest.addrs, ["10.0.0.3:5000"]);
assert.equal(p.guest.seen, "198.51.100.9");
await call("addrs", { code: h.code, key: g.key, addrs: ["198.51.100.9:41000", "10.0.0.3:5000"] }, "198.51.100.9");
p = await call("poll", { code: h.code, key: h.key });
assert.deepEqual(p.guest.addrs, ["198.51.100.9:41000", "10.0.0.3:5000"]);
p = await call("poll", { code: h.code, key: g.key }, "198.51.100.9");
assert.equal(p.host.name, "Naruto");

// keys: a wrong one is no room; the guest cannot ask for TURN; without a TURN key there is none
assert.equal((await call("poll", { code: h.code, key: "nope" })).error, "no room");
assert.equal((await call("poll", { code: h.code, key: "0123456789abcdef0123456789abcdef" })).error, "no room");
assert.equal((await call("relay", { code: h.code, key: g.key })).status, 403);
assert.match((await call("relay", { code: h.code, key: h.key })).noRelay, /TURN key/);

// the room's relay: a WebSocket only, by key, and the Durable Object told who is who
let r = await handle(new Request(`https://t/v1/ws?code=${h.code}&key=${h.key}`), env, now);
assert.equal(r.status, 426);
r = await handle(new Request(`https://t/v1/ws?code=${h.code}&key=${h.key}`, { headers: { upgrade: "websocket" } }), env, now);
assert.equal(r.status, 200);
r = await handle(new Request(`https://t/v1/ws?code=${h.code}&key=${g.key}`, { headers: { upgrade: "websocket" } }), env, now);
r = await handle(new Request(`https://t/v1/ws?code=${h.code}&key=bad`, { headers: { upgrade: "websocket" } }), env, now);
assert.equal(r.status, 404);
assert.deepEqual(relayCalls.map((c) => [c.room, c.who]), [[h.code, "h"], [h.code, "g"]]);

// the guest leaving frees the room for another
await call("leave", { code: h.code, key: g.key }, "198.51.100.9");
p = await call("poll", { code: h.code, key: h.key });
assert.equal(p.guest, null);
const g2 = await call("join", { v, game: GAME, fp, code: h.code, name: "Sakura", addrs: [] }, "198.51.100.11");
assert.equal(g2.status, 200);

// a room nobody asks about is gone
now += 100;
assert.equal((await call("poll", { code: h.code, key: h.key })).guest.name, "Sakura");
now += 200;
assert.equal((await call("poll", { code: h.code, key: h.key })).error, "no room");

// the host leaving closes the room
const h2 = await call("host", { v, game: GAME, fp, name: "H2", addrs: [] });
const j2 = await call("join", { v, game: GAME, fp, code: h2.code, name: "G", addrs: [] });
await call("leave", { code: h2.code, key: h2.key });
assert.equal((await call("poll", { code: h2.code, key: j2.key })).error, "no room");

// one address cannot hold more than ROOMS_PER_ADDRESS rooms
for (let i = 0; i < 4; i++) assert.equal((await call("host", { v, game: GAME, fp, name: "Spam", addrs: [] }, "192.0.2.50")).status, 200);
assert.equal((await call("host", { v, game: GAME, fp, name: "Spam", addrs: [] }, "192.0.2.50")).error, "busy");

// ---- quick match
const quick = (name, who, ip, f = fp) => call("quick", { v, game: GAME, fp: f, name, addrs: ["10.1.0." + name.length + ":6000"], opts: { delay: 1, relay: true }, who }, ip);
const fpMod = "app42+mods:un6_a35";
now += 1000; // (the rooms above are long gone)

// refused: another game or version, no `who`
assert.equal((await call("quick", { v: 7, game: GAME, fp, name: "Q", addrs: [], who: "abcdefgh1" })).error, "version");
assert.equal((await call("quick", { v, game: "bt2", fp, name: "Q", addrs: [], who: "abcdefgh1" })).error, "game");
assert.equal((await call("quick", { v, game: GAME, fp, name: "Q", addrs: [] })).status, 400);
assert.equal((await call("hello", { v, game: GAME, fp })).waiting, 0);
assert.equal((await call("hello", { v, game: GAME })).waiting, undefined);

// nobody waits: a room of one's own, in the queue (the first quick after 1000s also sweeps the 4 old spam rooms; subsequent ones write only 2 rows)
written = 0;
let qa = await quick("Naruto", "whoAAAAAAAA", "203.0.113.20");
assert.equal(qa.role, "host");
assert.match(qa.code, /^\d{6}$/);
assert.equal(written, 6);
assert.equal((await call("hello", { v, game: GAME, fp })).waiting, 1);
assert.equal((await call("hello", { v, game: GAME, fp: fpMod })).waiting, 0);
// its polls in the same seconds write nothing
written = 0;
assert.equal((await call("poll", { code: qa.code, key: qa.key }, "203.0.113.20")).guest, null);
assert.equal(written, 0);

// another build (other mods) never meets it: a room of its own (writes only 2 rows: room + queue)
written = 0;
const qm = await quick("Modded", "whoMMMMMMMM", "203.0.113.21", fpMod);
assert.equal(qm.role, "host");
assert.notEqual(qm.code, qa.code);
assert.equal(written, 2);

// the same phone searching again: its old room is gone, a new one waits (it is never given its own)
const qa2 = await quick("Naruto", "whoAAAAAAAA", "203.0.113.20");
assert.equal(qa2.role, "host");
assert.notEqual(qa2.code, qa.code);
assert.equal((await call("poll", { code: qa.code, key: qa.key }, "203.0.113.20")).error, "no room");
qa = qa2;

// a second player with the same build: put in the first one's room as its guest (two rows: the guest, the queue's place)
written = 0;
const qb = await quick("Sasuke", "whoBBBBBBBB", "198.51.100.30");
assert.equal(qb.role, "join");
assert.equal(qb.code, qa.code);
assert.equal(qb.host.name, "Naruto");
assert.equal(qb.host.seen, "203.0.113.20");
assert.deepEqual(qb.opts, { delay: 1, relay: true });
assert.equal(written, 2);
// from here it is a room like any other: the host's poll has the guest, the guest's has the host, the relay takes both
p = await call("poll", { code: qa.code, key: qa.key }, "203.0.113.20");
assert.equal(p.guest.name, "Sasuke");
assert.equal(p.guest.seen, "198.51.100.30");
assert.equal((await call("poll", { code: qa.code, key: qb.key }, "198.51.100.30")).host.name, "Naruto");
relayCalls.length = 0;
await handle(new Request(`https://t/v1/ws?code=${qa.code}&key=${qb.key}`, { headers: { upgrade: "websocket" } }), env, now);
assert.deepEqual(relayCalls.map((c) => [c.room, c.who]), [[qa.code, "g"]]);
// and it is out of the queue: a third player waits on its own
assert.equal((await call("hello", { v, game: GAME, fp })).waiting, 0);
const qc = await quick("Sakura", "whoCCCCCCCC", "198.51.100.31");
assert.equal(qc.role, "host");

// a waiting room is kept by its host's polls; one not polled for QUICK_IDLE seconds is given to nobody and goes
for (let i = 0; i < 6; i++) {
    now += 5;
    assert.equal((await call("poll", { code: qc.code, key: qc.key }, "198.51.100.31")).guest, null);
}
assert.equal((await call("hello", { v, game: GAME, fp })).waiting, 1);
assert.equal((await call("hello", { v, game: GAME, fp: fpMod })).waiting, 0); // (the modded one stopped polling long ago)
const qd = await quick("Rock Lee", "whoDDDDDDDD", "198.51.100.32");
assert.equal(qd.role, "join");
assert.equal(qd.code, qc.code);
const qm2 = await quick("Modded2", "whoNNNNNNNN", "198.51.100.33", fpMod);
assert.equal(qm2.role, "host"); // (not the stale one)
assert.equal((await call("poll", { code: qm.code, key: qm.key }, "203.0.113.21")).error, "no room");
await call("leave", { code: qm2.code, key: qm2.key }, "198.51.100.33");
assert.equal((await call("hello", { v, game: GAME, fp: fpMod })).waiting, 0);

// ---- input_mode separation (touch vs gamepad) + session token
now += 100;
const touchRoom = await call("host", { v, game: GAME, fp, name: "TouchH", addrs: [], input_mode: "touch" }, "198.51.100.60");
assert.equal(touchRoom.status, 200);
assert.equal(touchRoom.input_mode, "touch");
assert.match(touchRoom.token, /^[0-9a-f]{32}$/);
assert.equal((await call("join", { v, game: GAME, fp, code: touchRoom.code, name: "PadG", addrs: [], input_mode: "gamepad" }, "198.51.100.61")).error, "input_mode");
const touchJoin = await call("join", { v, game: GAME, fp, code: touchRoom.code, name: "TouchG", addrs: [], input_mode: "touch" }, "198.51.100.62");
assert.equal(touchJoin.status, 200);
assert.equal(touchJoin.token, touchRoom.token);
assert.equal(touchJoin.input_mode, "touch");

// quick match: touch never meets gamepad, touch meets touch
const qt = await call("quick", { v, game: GAME, fp, name: "TouchQ1", addrs: [], input_mode: "touch", who: "whoTOUCH1111" }, "198.51.100.70");
const qp = await call("quick", { v, game: GAME, fp, name: "PadQ1", addrs: [], input_mode: "gamepad", who: "whoPAD111111" }, "198.51.100.71");
assert.equal(qt.role, "host");
assert.equal(qp.role, "host");
assert.notEqual(qt.code, qp.code);
assert.equal((await call("hello", { v, game: GAME, fp, input_mode: "touch" })).waiting, 1);
assert.equal((await call("hello", { v, game: GAME, fp, input_mode: "gamepad" })).waiting, 1);
const qt2 = await call("quick", { v, game: GAME, fp, name: "TouchQ2", addrs: [], input_mode: "touch", who: "whoTOUCH2222" }, "198.51.100.72");
assert.equal(qt2.role, "join");
assert.equal(qt2.code, qt.code);
assert.equal(qt2.token, qt.token);

// two searching in the same moment both find nobody: the second gives its room up and joins the first
const race = "app43+mods:none";
let qf = null;
beforeQueue = async () => { qf = await quick("First", "whoFFFFFFFF", "198.51.100.40", race); };
const qg = await quick("Second", "whoGGGGGGGG", "198.51.100.41", race);
assert.equal(qf.role, "host");
assert.equal(qg.role, "join");
assert.equal(qg.code, qf.code);
assert.equal((await call("poll", { code: qf.code, key: qf.key }, "198.51.100.40")).guest.name, "Second");
assert.equal((await call("hello", { v, game: GAME, fp: race })).waiting, 0);
assert.equal(db.prepare("SELECT COUNT(*) AS n FROM hl_rooms WHERE fp = ?").get(race).n, 1);

// D1's free limits used up: the app is told "quota" (429), not a plain failure
assert.equal(quotaError("D1_ERROR: Exceeded maximum daily rows written"), true);
assert.equal(quotaError("D1_ERROR: no such table: hl_rooms"), false);
const spent = { DB: { prepare() { throw new Error("D1_ERROR: You have exceeded the daily limit of rows read"); } }, ALLOWED_VERSIONS: "1" };
let qr = await worker.fetch(new Request("https://t/v1/quick", { method: "POST", body: JSON.stringify({ v, game: GAME, fp, who: "whoZZZZZZZZ" }) }), spent);
assert.equal(qr.status, 429);
assert.equal((await qr.json()).error, "quota");
const broken = { DB: { prepare() { throw new Error("D1_ERROR: no such table: hl_rooms"); } }, ALLOWED_VERSIONS: "1" };
qr = await worker.fetch(new Request("https://t/v1/host", { method: "POST", body: JSON.stringify({ v, game: GAME, fp }) }), broken);
assert.equal(qr.status, 503);

// bad bodies
let bad = await handle(new Request("https://t/v1/host", { method: "POST", body: "not json" }), env, now);
assert.equal(bad.status, 400);
bad = await handle(new Request("https://t/v1/host", { method: "POST", body: "x".repeat(5000) }), env, now);
assert.equal(bad.status, 413);
bad = await handle(new Request("https://t/v1/nothing", { method: "POST", body: "{}" }), env, now);
assert.equal(bad.status, 404);

// the Durable Object: what the host sends reaches the guest and back, unchanged; spectators receive tagged fan-out and are read-only
const sockets = { h: [], g: [], s: [] };
const mk = (tag) => { const s = { tag, got: [], send(m) { this.got.push(new Uint8Array(m)); } }; sockets[tag].push(s); return s; };
const hs = mk("h"), gs = mk("g"), sp = mk("s");
const relay = new HiddenLeafRelay({ getWebSockets: (tag) => sockets[tag] ?? [], getTags: (ws) => [ws.tag] });
relay.webSocketMessage(hs, new Uint8Array([1, 2, 3]).buffer);
relay.webSocketMessage(gs, new Uint8Array([9]).buffer);
relay.webSocketMessage(sp, new Uint8Array([7, 7]).buffer); // ignored: spectators are read-only
relay.webSocketMessage(gs, "text");
relay.webSocketMessage(hs, new Uint8Array(0).buffer);
relay.webSocketMessage(hs, new Uint8Array(2000).buffer);
assert.deepEqual(gs.got.map((m) => [...m]), [[1, 2, 3]]);
assert.deepEqual(hs.got.map((m) => [...m]), [[9]]);
assert.deepEqual(sp.got.map((m) => [...m]), [[0x53, 1, 1, 2, 3], [0x53, 2, 9]]);

// ---- Phase 8: Ranked profiles, bilateral match verification, Global Top 100 leaderboard
const p1 = await call("profile", { player_id: "p1_deadbeef01", secret: "secret_p1_0123456789", name: "NarutoRP", input_mode: "touch" }, "198.51.100.80");
assert.equal(p1.status, 200);
assert.equal(p1.points, 1000);
assert.equal(p1.tier, "Genin");
assert.equal((await call("profile", { player_id: "p1_deadbeef01", secret: "wrong_secret_0123456789", name: "Hacker" })).status, 403);
const p2 = await call("profile", { player_id: "p2_deadbeef02", secret: "secret_p2_0123456789", name: "SasukeRP", input_mode: "touch" }, "198.51.100.81");
assert.equal(p2.status, 200);

// bilateral verification: first report waits, second matching report resolves and awards RP
const rep1 = await call("ranked/report", { match_id: "m_100001", player_id: "p1_deadbeef01", opponent_id: "p2_deadbeef02", secret: "secret_p1_0123456789", role: "p1", winner: "p1" });
assert.equal(rep1.resolved, false);
assert.equal(rep1.waiting, true);
const rep2 = await call("ranked/report", { match_id: "m_100001", player_id: "p2_deadbeef02", opponent_id: "p1_deadbeef01", secret: "secret_p2_0123456789", role: "p2", winner: "p1" });
assert.equal(rep2.resolved, true);
assert.equal(rep2.winner_id, "p1_deadbeef01");
assert.ok(rep2.p1_points > 1000);
assert.ok(rep2.p2_points < 1000);

// disputed report (cheater claims opposite winner) -> 0 points awarded
await call("ranked/report", { match_id: "m_100002", player_id: "p1_deadbeef01", opponent_id: "p2_deadbeef02", secret: "secret_p1_0123456789", role: "p1", winner: "p1" });
const disp = await call("ranked/report", { match_id: "m_100002", player_id: "p2_deadbeef02", opponent_id: "p1_deadbeef01", secret: "secret_p2_0123456789", role: "p2", winner: "p2" });
assert.equal(disp.resolved, false);
assert.equal(disp.disputed, true);

// cached global leaderboard
const lbRes = await handle(new Request("https://t/v1/leaderboard?limit=10&mode=touch"), env, now);
assert.equal(lbRes.status, 200);
assert.match(lbRes.headers.get("cache-control"), /max-age=60/);
const lb = await lbRes.json();
assert.equal(lb.entries[0].name, "NarutoRP");
assert.equal(lb.entries[0].rank, 1);

// ---- Phase 8: Custom Lobby (2-8 players, King-of-the-Hill rotation + spectator WS)
const lob = await call("lobby/create", { v, game: GAME, fp, player_id: "host_0001", name: "Kakashi", max_players: 4, allow_spectators: true, input_mode: "any" }, "198.51.100.90");
assert.equal(lob.status, 200);
assert.equal(lob.members.length, 1);
const lj1 = await call("lobby/join", { v, game: GAME, fp, code: lob.code, player_id: "ply_0002", name: "Guy" }, "198.51.100.91");
assert.equal(lj1.members.find((m) => m.id === "ply_0002").role, "p2");
const lj2 = await call("lobby/join", { v, game: GAME, fp, code: lob.code, player_id: "ply_0003", name: "Lee" }, "198.51.100.92");
assert.equal(lj2.members.find((m) => m.id === "ply_0003").role, "queue");
const lSpec = await call("lobby/join", { v, game: GAME, fp, code: lob.code, player_id: "spec_0004", name: "Tenten", spectator: true }, "198.51.100.93");
assert.equal(lSpec.members.find((m) => m.id === "spec_0004").role, "spectator");
// King-of-the-Hill rotation: p2 (Guy) wins -> Guy becomes p1, Lee becomes p2, Kakashi goes to queue!
const rot = await call("lobby/rotate", { code: lob.code, player_id: "host_0001", winner: "p2" }, "198.51.100.90");
assert.equal(rot.members.find((m) => m.id === "ply_0002").role, "p1");
assert.equal(rot.members.find((m) => m.id === "ply_0003").role, "p2");
assert.equal(rot.members.find((m) => m.id === "host_0001").role, "queue");

// Spectator WebSocket upgrade
const specRoom = await call("host", { v, game: GAME, fp, name: "SpecHost", addrs: [], opts: { spectators: true } }, "198.51.100.95");
relayCalls.length = 0;
const wsSpec = await handle(new Request(`https://t/v1/ws?code=${specRoom.code}&role=spectator`, { headers: { upgrade: "websocket" } }), env, now);
assert.equal(wsSpec.status, 200);
assert.deepEqual(relayCalls.map((c) => [c.room, c.who]), [[specRoom.code, "s"]]);
console.log("hiddenleaf-online worker: all tests passed");

console.log("hiddenleaf-online worker: all tests passed");
