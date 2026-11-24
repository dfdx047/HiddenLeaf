// Local live dev server for testing Hidden Leaf Online (worker.js) in real time on Android devices over Wi-Fi.
// Includes built-in RFC 6455 WebSocket relay (/v1/ws) with live Spectator fan-out and late-join history replay.
// Run: node online/worker/dev_server.mjs [port=8787]
import http from "node:http";
import crypto from "node:crypto";
import fs from "node:fs";
import { DatabaseSync } from "node:sqlite";
import { handle } from "./worker.js";

const PORT = Number(process.argv[2] || 8787);
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
};

const env = { DB: D1, ALLOWED_VERSIONS: "1" };

// ---- In-memory WebSocket relay matching HiddenLeafRelay in worker.js
const relays = new Map(); // code -> { h: Set<sock>, g: Set<sock>, s: Set<sock>, specHistory: Buffer[] }
function getRelay(code) {
    let r = relays.get(code);
    if (!r) {
        r = { h: new Set(), g: new Set(), s: new Set(), specHistory: [] };
        relays.set(code, r);
    }
    return r;
}

function wsSendFrame(socket, opcode, payload) {
    if (socket.destroyed) return;
    const len = payload.length;
    let header;
    if (len < 126) {
        header = Buffer.from([0x80 | opcode, len]);
    } else if (len < 65536) {
        header = Buffer.alloc(4);
        header[0] = 0x80 | opcode;
        header[1] = 126;
        header.writeUInt16BE(len, 2);
    } else {
        return;
    }
    try { socket.write(Buffer.concat([header, payload])); } catch {}
}

function attachWs(socket, code, who) {
    const r = getRelay(code);
    const bucket = r[who];
    if (who === "h" || who === "g") {
        for (const old of bucket) { try { old.destroy(); } catch {} }
        bucket.clear();
    }
    bucket.add(socket);
    console.log(`[ws] room ${code}: ${who} connected (h=${r.h.size} g=${r.g.size} s=${r.s.size}, hist=${r.specHistory.length})`);

    // Replay buffered SP frames in 50-frame chunks so late-joining spectators catch up to live
    if (who === "s" && r.specHistory.length > 0) {
        for (let i = 0; i < r.specHistory.length; i += 50) {
            const chunk = Buffer.concat(r.specHistory.slice(i, i + 50));
            wsSendFrame(socket, 0x2, chunk);
        }
    }

    let buf = Buffer.alloc(0);
    socket.on("data", (chunk) => {
        buf = Buffer.concat([buf, chunk]);
        while (buf.length >= 2) {
            const opcode = buf[0] & 0x0f;
            const masked = (buf[1] & 0x80) !== 0;
            let len = buf[1] & 0x7f;
            let off = 2;
            if (len === 126) {
                if (buf.length < 4) break;
                len = buf.readUInt16BE(2);
                off = 4;
            } else if (len === 127) {
                if (buf.length < 10) break;
                len = Number(buf.readBigUInt64BE(2));
                off = 10;
            }
            const maskLen = masked ? 4 : 0;
            if (buf.length < off + maskLen + len) break;
            const mask = masked ? buf.subarray(off, off + 4) : null;
            off += maskLen;
            const payload = Buffer.from(buf.subarray(off, off + len));
            buf = buf.subarray(off + len);
            if (mask) {
                for (let i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];
            }
            if (opcode === 0x8) {
                wsSendFrame(socket, 0x8, Buffer.alloc(0));
                socket.end();
                break;
            } else if (opcode === 0x9) {
                wsSendFrame(socket, 0xA, payload);
            } else if (opcode === 0x2 && (who === "h" || who === "g")) {
                // Host spectator frame ('S','P' + 18 bytes): store & broadcast ONLY to spectators
                if (who === "h" && payload.length === 20 && payload[0] === 0x53 && payload[1] === 0x50) {
                    if (r.specHistory.length < 15000) r.specHistory.push(payload);
                    for (const sp of r.s) wsSendFrame(sp, 0x2, payload);
                } else {
                    const other = who === "h" ? r.g : r.h;
                    for (const peer of other) wsSendFrame(peer, 0x2, payload);
                }
            }
        }
    });
    const cleanup = () => {
        bucket.delete(socket);
        if (r.h.size === 0 && r.g.size === 0 && r.s.size === 0) {
            // Keep specHistory briefly? Clear when room is completely empty
            setTimeout(() => {
                if (r.h.size === 0 && r.g.size === 0 && r.s.size === 0) relays.delete(code);
            }, 30000);
        }
    };
    socket.on("close", cleanup);
    socket.on("error", cleanup);
}

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

server.on("upgrade", (req, socket) => {
    try {
        const u = new URL(req.url, `http://${req.headers.host || "localhost"}`);
        if (u.pathname !== "/v1/ws") { socket.destroy(); return; }
        const code = (u.searchParams.get("code") || "").replace(/\D/g, "");
        const key = u.searchParams.get("key") || "";
        const role = u.searchParams.get("role") || "";
        const room = db.prepare("SELECT * FROM hl_rooms WHERE code = ?").get(code);
        if (!room) { socket.write("HTTP/1.1 404 Not Found\r\n\r\n"); socket.destroy(); return; }
        let who = "";
        if (role === "spectator") who = "s";
        else if (key && key === room.host_key) who = "h";
        else if (key && key === room.guest_key) who = "g";
        else { socket.write("HTTP/1.1 403 Forbidden\r\n\r\n"); socket.destroy(); return; }

        const wsKey = req.headers["sec-websocket-key"];
        if (!wsKey) { socket.destroy(); return; }
        const accept = crypto.createHash("sha1").update(wsKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").digest("base64");
        socket.write(
            "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            `Sec-WebSocket-Accept: ${accept}\r\n\r\n`
        );
        socket.setNoDelay(true);
        attachWs(socket, code, who);
    } catch {
        socket.destroy();
    }
});

server.listen(PORT, "0.0.0.0", () => {
    console.log(`Hidden Leaf Online live dev server listening on http://0.0.0.0:${PORT} (HTTP + WS Spectator Relay)`);
});
