-- Hidden Leaf Online (worker.js): run once in the D1 database
-- (wrangler d1 execute hiddenleaf-online --remote --file schema.sql, or paste it in the database's Console).
CREATE TABLE IF NOT EXISTS hl_rooms (
    code TEXT PRIMARY KEY,
    v INTEGER NOT NULL,
    game TEXT NOT NULL,
    fp TEXT NOT NULL DEFAULT '',
    host_key TEXT NOT NULL,
    host_name TEXT NOT NULL DEFAULT '',
    host_addrs TEXT NOT NULL DEFAULT '[]',
    host_seen TEXT NOT NULL DEFAULT '',
    guest_key TEXT,
    guest_name TEXT,
    guest_addrs TEXT,
    guest_seen TEXT,
    opts TEXT NOT NULL DEFAULT '{}',
    created INTEGER NOT NULL,
    touched INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS hl_rooms_touched ON hl_rooms (touched);
CREATE INDEX IF NOT EXISTS hl_rooms_host_seen ON hl_rooms (host_seen);
CREATE TABLE IF NOT EXISTS hl_relay_days (
    day INTEGER PRIMARY KEY,
    n INTEGER NOT NULL
);
