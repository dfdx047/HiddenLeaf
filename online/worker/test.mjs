// The Worker against a real SQLite database standing in for D1 (Node 22+: node:sqlite). Run: node test.mjs
// Nothing is sent anywhere: the Durable Object and the TURN service are stand-ins.
import { DatabaseSync } from "node:sqlite";
import fs from "node:fs";
import assert from "node:assert/strict";
import { handle, HiddenLeafRelay, GAME } from "./worker.js";

const db = new DatabaseSync(":memory:");
db.exec(fs.readFileSync(new URL("./schema.sql", import.meta.url), "utf8"));
const D1 = {
    prepare(sql) {
        return {
            args: [],
            bind(...a) { this.args = a; return this; },
            async first() { return db.prepare(sql).get(...this.args) ?? null; },
            async all() { return { results: db.prepare(sql).all(...this.args) }; },
            async run() { const r = db.prepare(sql).run(...this.args); return { meta: { changes: r.changes } }; },
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

// bad bodies
let bad = await handle(new Request("https://t/v1/host", { method: "POST", body: "not json" }), env, now);
assert.equal(bad.status, 400);
bad = await handle(new Request("https://t/v1/host", { method: "POST", body: "x".repeat(5000) }), env, now);
assert.equal(bad.status, 413);
bad = await handle(new Request("https://t/v1/nothing", { method: "POST", body: "{}" }), env, now);
assert.equal(bad.status, 404);

// the Durable Object: what the host sends reaches the guest and back, unchanged; text and empty messages are dropped
const sockets = { h: [], g: [] };
const mk = (tag) => { const s = { tag, got: [], send(m) { this.got.push(new Uint8Array(m)); } }; sockets[tag].push(s); return s; };
const hs = mk("h"), gs = mk("g");
const relay = new HiddenLeafRelay({ getWebSockets: (tag) => sockets[tag] ?? [], getTags: (ws) => [ws.tag] });
relay.webSocketMessage(hs, new Uint8Array([1, 2, 3]).buffer);
relay.webSocketMessage(gs, new Uint8Array([9]).buffer);
relay.webSocketMessage(gs, "text");
relay.webSocketMessage(hs, new Uint8Array(0).buffer);
relay.webSocketMessage(hs, new Uint8Array(2000).buffer);
assert.deepEqual(gs.got.map((m) => [...m]), [[1, 2, 3]]);
assert.deepEqual(hs.got.map((m) => [...m]), [[9]]);

console.log("hiddenleaf-online worker: all tests passed");
