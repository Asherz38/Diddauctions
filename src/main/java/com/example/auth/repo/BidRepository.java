package com.example.auth.repo;

import com.example.auth.model.TopBid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BidRepository {
  private final JdbcTemplate jdbc;

  public BidRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void placeBid(int itemId, int userId, double amount) {
    jdbc.update("INSERT INTO bids(item_id, user_id, amount) VALUES (?,?,?)", itemId, userId, amount);
    jdbc.update(
        "UPDATE items SET current_price = CASE WHEN ? > ifnull(current_price, starting_price) THEN ? ELSE current_price END WHERE id = ?",
        amount, amount, itemId);
  }

  public Optional<TopBid> topBid(int itemId) {
    String sql = "SELECT b.amount, u.username FROM bids b JOIN users u ON u.id = b.user_id " +
        "WHERE b.item_id = ? ORDER BY b.amount DESC, b.id ASC LIMIT 1";
    var list = jdbc.query(sql, (rs, i) -> new TopBid(rs.getDouble(1), rs.getString(2)), itemId);
    return list.stream().findFirst();
  }

  public void buyNow(int itemId, int userId) {
    Double price = jdbc.queryForObject("SELECT COALESCE(current_price, starting_price) FROM items WHERE id=?",
        Double.class, itemId);
    if (price == null)
      price = 0d;
    jdbc.update("INSERT INTO bids(item_id, user_id, amount) VALUES (?,?,?)", itemId, userId, price);
    jdbc.update("UPDATE items SET current_price=?, status='CLOSED', closes_at=datetime('now') WHERE id=?", price,
        itemId);
  }
}


