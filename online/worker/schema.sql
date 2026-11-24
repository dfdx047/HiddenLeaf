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

-- ranked players & global leaderboard (/v1/profile, /v1/ranked/report, /v1/leaderboard)
CREATE TABLE IF NOT EXISTS hl_players (
    player_id TEXT PRIMARY KEY,
    auth_hash TEXT NOT NULL,
    name TEXT NOT NULL DEFAULT '',
    points INTEGER NOT NULL DEFAULT 1000,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    streak INTEGER NOT NULL DEFAULT 0,
    input_mode TEXT NOT NULL DEFAULT 'any',
    fav_char TEXT NOT NULL DEFAULT 'naruto',
    fav_char_lvl INTEGER NOT NULL DEFAULT 1,
    fav_char_xp INTEGER NOT NULL DEFAULT 0,
    updated INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS hl_players_rank ON hl_players (points DESC, wins DESC);

-- bilateral ranked match verification (prevents fake win reports)
CREATE TABLE IF NOT EXISTS hl_matches (
    match_id TEXT PRIMARY KEY,
    p1_id TEXT NOT NULL,
    p2_id TEXT NOT NULL,
    p1_claim TEXT,
    p2_claim TEXT,
    winner_id TEXT,
    p1_delta INTEGER NOT NULL DEFAULT 0,
    p2_delta INTEGER NOT NULL DEFAULT 0,
    resolved INTEGER NOT NULL DEFAULT 0,
    created INTEGER NOT NULL
);

-- custom lobbies (2-8 players, King-of-the-Hill queue + spectator support)
CREATE TABLE IF NOT EXISTS hl_lobbies (
    code TEXT PRIMARY KEY,
    v INTEGER NOT NULL,
    game TEXT NOT NULL,
    fp TEXT NOT NULL DEFAULT '',
    host_id TEXT NOT NULL,
    input_mode TEXT NOT NULL DEFAULT 'any',
    max_players INTEGER NOT NULL DEFAULT 4,
    allow_spectators INTEGER NOT NULL DEFAULT 1,
    members TEXT NOT NULL DEFAULT '[]',
    active_room TEXT,
    created INTEGER NOT NULL,
    touched INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS hl_lobbies_touched ON hl_lobbies (touched);
