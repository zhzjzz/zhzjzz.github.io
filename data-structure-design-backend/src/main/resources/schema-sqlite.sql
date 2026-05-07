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
    published_at TEXT,
    score REAL,
    views INTEGER,
    destination_id INTEGER
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
