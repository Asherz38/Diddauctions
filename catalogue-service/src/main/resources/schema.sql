CREATE TABLE IF NOT EXISTS items (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT,
  status TEXT NOT NULL CHECK (status IN ('UPCOMING','ACTIVE','CLOSED')),
  starting_price REAL DEFAULT 0,
  current_price REAL DEFAULT 0,
  auction_type TEXT NOT NULL CHECK (auction_type IN ('FORWARD','DUTCH')) DEFAULT 'FORWARD',
  closes_at TEXT,
  shipping_price REAL DEFAULT 0,
  expedite_price REAL DEFAULT 0,
  shipping_days INTEGER DEFAULT 5,
  seller_user_id INTEGER,
  keywords TEXT
);


CREATE TABLE IF NOT EXISTS item_keywords (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  item_id INTEGER NOT NULL,
  keyword TEXT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(item_id, keyword)
);


CREATE INDEX IF NOT EXISTS idx_items_status ON items(status);
CREATE INDEX IF NOT EXISTS idx_items_name ON items(name);
CREATE INDEX IF NOT EXISTS idx_items_auction ON items(auction_type);
CREATE INDEX IF NOT EXISTS idx_items_closes ON items(closes_at);
CREATE INDEX IF NOT EXISTS idx_item_keywords_kw ON item_keywords(keyword);

