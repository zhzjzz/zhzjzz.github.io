CREATE TABLE IF NOT EXISTS destination (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    scene_type TEXT,
    category TEXT,
    heat REAL,
    rating REAL,
    description TEXT,
    latitude REAL,
    longitude REAL
);

CREATE TABLE IF NOT EXISTS user_account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name TEXT NOT NULL,
    interests TEXT
);

CREATE TABLE IF NOT EXISTS diary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT,
    media_url TEXT,
    media_type TEXT,
    compressed_media_url TEXT,
    original_size_bytes INTEGER DEFAULT 0,
    compressed_size_bytes INTEGER DEFAULT 0,
    compression_status TEXT DEFAULT 'pending',
    aigc_animation_url TEXT,
    aigc_status TEXT DEFAULT 'pending',
    heat_score REAL DEFAULT 0,
    like_count INTEGER DEFAULT 0,
    favorite_count INTEGER DEFAULT 0,
    comment_count INTEGER DEFAULT 0,
    share_count INTEGER DEFAULT 0,
    is_public INTEGER DEFAULT 1,
    share_token TEXT,
    author_name TEXT,
    published_at TEXT,
    score REAL,
    views INTEGER,
    destination_id INTEGER
);

CREATE TABLE IF NOT EXISTS diary_comment (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    diary_id INTEGER NOT NULL,
    author_name TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS facility (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    facility_type TEXT,
    latitude REAL,
    longitude REAL,
    destination_id INTEGER
);

CREATE TABLE IF NOT EXISTS food (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    cuisine TEXT,
    store_name TEXT,
    rating REAL,
    heat REAL,
    average_price REAL,
    latitude REAL,
    longitude REAL,
    source_type TEXT,
    source_id TEXT,
    image_url TEXT,
    destination_id INTEGER
);

CREATE TABLE IF NOT EXISTS itinerary (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,
    owner TEXT,
    collaborators TEXT,
    strategy TEXT,
    transport_mode TEXT,
    notes TEXT,
    updated_at TEXT
);

CREATE TABLE IF NOT EXISTS itinerary_spot_vote (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    itinerary_id INTEGER NOT NULL,
    spot_id INTEGER NOT NULL,
    spot_name TEXT NOT NULL,
    username TEXT NOT NULL,
    vote_type TEXT NOT NULL,
    reason TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_itinerary_spot_vote_unique
ON itinerary_spot_vote(itinerary_id, spot_id, username);

CREATE TABLE IF NOT EXISTS itinerary_spot_candidate (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    itinerary_id INTEGER NOT NULL,
    destination_id INTEGER NOT NULL,
    spot_name TEXT NOT NULL,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_itinerary_spot_candidate_unique
ON itinerary_spot_candidate(itinerary_id, destination_id);

CREATE INDEX IF NOT EXISTS idx_itinerary_spot_candidate_itinerary
ON itinerary_spot_candidate(itinerary_id);
