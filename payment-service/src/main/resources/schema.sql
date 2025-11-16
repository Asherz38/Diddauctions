CREATE TABLE IF NOT EXISTS payments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  item_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  winner_amount REAL NOT NULL,
  total REAL NOT NULL,
  expedited INTEGER NOT NULL,
  closes_at TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payments_item ON payments(item_id);
CREATE INDEX IF NOT EXISTS idx_payments_user ON payments(user_id);

