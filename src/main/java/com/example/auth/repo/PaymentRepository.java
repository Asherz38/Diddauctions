package com.example.auth.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PaymentRepository {
  private final JdbcTemplate jdbc;

  public PaymentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long record(int itemId, int userId, double total, boolean expedited) {
    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
  "INSERT INTO payments(item_id, user_id, total, expedited) VALUES (?,?,?,?)",
      Statement.RETURN_GENERATED_KEYS);

      ps.setInt(1, itemId);
      
      ps.setInt(2, userId);

      ps.setDouble(3, total);

      ps.setInt(4, expedited ? 1 : 0);

      return ps;

    }, kh);
    Number key = kh.getKey();
    
    return key == null ? -1L : key.longValue();
  }
}





