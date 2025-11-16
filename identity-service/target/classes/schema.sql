CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  first_name TEXT NOT NULL,
  last_name TEXT NOT NULL,
  street_name TEXT NOT NULL,
  street_number TEXT NOT NULL,
  city TEXT NOT NULL,
  country TEXT NOT NULL,
  postal_code TEXT NOT NULL,
  twofa_secret TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_lastname ON users(last_name);

