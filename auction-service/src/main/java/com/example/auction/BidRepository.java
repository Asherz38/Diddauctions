package com.example.auction;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class BidRepository {
  private final JdbcTemplate jdbc;
  public BidRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

  public void placeBid(int itemId, int userId, double amount){
    jdbc.update("INSERT INTO bids(item_id,user_id,amount) VALUES (?,?,?)", itemId, userId, amount);
  }

  public Optional<Map<String,Object>> topBid(int itemId){
    var list = jdbc.query("SELECT amount, user_id FROM bids WHERE item_id=? ORDER BY amount DESC, id ASC LIMIT 1",
      (rs,i) -> {
        Map<String,Object> m = new java.util.HashMap<>();
        m.put("amount", rs.getDouble(1));
        m.put("userId", rs.getInt(2));
        return m;
      }, itemId);
    return list.stream().findFirst();
  }
}
