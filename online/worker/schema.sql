-- Hidden Leaf Online (worker.js): run once in the D1 database
-- (wrangler d1 execute hiddenleaf-online --remote --file schema.sql, or paste it in the database's Console).
CREATE TABLE IF NOT EXISTS hl_rooms (
    code TEXT PRIMARY KEY,
    v INTEGER NOT NULL,
    game TEXT NOT NULL,
    fp TEXT NOT NULL DEFAULT '',
    input_mode TEXT NOT NULL DEFAULT 'any',
    session_secret TEXT NOT NULL DEFAULT '',
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
-- quick match (/v1/quick): the rooms that wait for a random opponent, the longest waiting first (by rowid);
-- a room leaves it when someone takes it, its host stops polling, or it is gone
CREATE TABLE IF NOT EXISTS hl_queue (
    code TEXT PRIMARY KEY,
    v INTEGER NOT NULL,
    game TEXT NOT NULL,
    fp TEXT NOT NULL DEFAULT '',
    input_mode TEXT NOT NULL DEFAULT 'any',
    who TEXT NOT NULL,
    made INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS hl_queue_match ON hl_queue (v, game, fp, input_mode);
