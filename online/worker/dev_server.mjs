// Local live dev server for testing Hidden Leaf Online (worker.js) in real time on Android devices over Wi-Fi.
// Run: node online/worker/dev_server.mjs [port=8787]
import http from "node:http";
import fs from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { handle } from "./worker.js";

const PORT = Number(process.argv[2] || 8787);
const db = new DatabaseSync(":memory:");
db.exec(fs.readFileSync(new URL("./schema.sql", import.meta.url), "utf8"));

// Seed realistic Top 100 leaderboard entries so the Ranked UI can be tested live immediately
const now = Math.floor(Date.now() / 1000);
const seed = [
    ["seed_01", "hash1", "Naruto_BR", 2140, 48, 6, 9, "gamepad", "naruto", 42, 8250],
    ["seed_02", "hash2", "SasukeUchiha", 1920, 41, 11, 5, "touch", "sasuke", 37, 7280],
    ["seed_03", "hash3", "KakashiCopy", 1765, 35, 12, 4, "gamepad", "kakashi", 31, 6090],
    ["seed_04", "hash4", "RockLee8G", 1580, 29, 14, 3, "touch", "lee", 26, 5120],
    ["seed_05", "hash5", "NejiHyuga", 1390, 22, 13, 2, "touch", "neji", 21, 4050],
    ["seed_06", "hash6", "GaaraSuna", 1185, 15, 11, 1, "gamepad", "gaara", 14, 2680],
    ["seed_07", "hash7", "SakuraHaruno", 1045, 8, 6, 0, "touch", "sakura", 9, 1650],
];
const ins = db.prepare("INSERT INTO hl_players (player_id, auth_hash, name, points, wins, losses, streak, input_mode, fav_char, fav_char_lvl, fav_char_xp, updated) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
for (const r of seed) ins.run(...r, now);

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
};

const env = { DB: D1, ALLOWED_VERSIONS: "1" };

const server = http.createServer(async (req, res) => {
    try {
        const chunks = [];
        for await (const c of req) chunks.push(c);
        const bodyBuf = Buffer.concat(chunks);
        const ip = (req.socket.remoteAddress || "127.0.0.1").replace(/^::ffff:/, "");
        const url = `http://${req.headers.host || "localhost"}${req.url}`;
        const init = {
            method: req.method,
            headers: { ...req.headers, "cf-connecting-ip": ip },
        };
        if (req.method !== "GET" && req.method !== "HEAD" && bodyBuf.length > 0) init.body = bodyBuf;
        const r = await handle(new Request(url, init), env, Math.floor(Date.now() / 1000));
        res.statusCode = r.status;
        for (const [k, v] of r.headers.entries()) res.setHeader(k, v);
        res.end(Buffer.from(await r.arrayBuffer()));
    } catch (e) {
        res.statusCode = 500;
        res.setHeader("content-type", "application/json");
        res.end(JSON.stringify({ error: String(e.message || e) }));
    }
});

server.listen(PORT, "0.0.0.0", () => {
    console.log(`Hidden Leaf Online live dev server listening on http://0.0.0.0:${PORT}`);
});
