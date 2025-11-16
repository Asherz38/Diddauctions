CREATE TABLE IF NOT EXISTS bids (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  item_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  amount REAL NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bids_item_created ON bids(item_id, created_at);
CREATE INDEX IF NOT EXISTS idx_bids_item_amount ON bids(item_id, amount);


CREATE TABLE IF NOT EXISTS auctions (
  item_id INTEGER PRIMARY KEY,
  closes_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_auctions_item ON auctions(item_id);


CREATE TABLE IF NOT EXISTS wins (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  item_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  amount REAL NOT NULL,
  ended_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_wins_item ON wins(item_id);
CREATE INDEX IF NOT EXISTS idx_wins_user ON wins(user_id);


