CREATE TABLE IF NOT EXISTS users (
	username TEXT PRIMARY KEY,
	password TEXT NOT NULL,
	email TEXT NOT NULL,
	access_level TEXT NOT NULL,
	banned INTEGER NOT NULL DEFAULT FALSE
);

INSERT OR IGNORE INTO users(username, password, email, access_level) VALUES('holocore', 'password', 'holocore@projectswg.com', 'csr');

CREATE TABLE IF NOT EXISTS players (
	id INTEGER PRIMARY KEY AUTOINCREMENT,
	name TEXT NOT NULL,
	race TEXT NOT NULL,
	username TEXT NOT NULL
);
