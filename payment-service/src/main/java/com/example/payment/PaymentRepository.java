package com.example.payment;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;

@Repository
public class PaymentRepository {
  private final JdbcTemplate jdbc;
  public PaymentRepository(JdbcTemplate jdbc){ 
    this.jdbc = jdbc; 
  }

  public long record(int itemId, int userId, double winnerAmount, double total, boolean expedited, String closesAt){
    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(con -> {
      PreparedStatement ps = con.prepareStatement(
        "INSERT INTO payments(item_id,user_id,winner_amount,total,expedited,closes_at) VALUES (?,?,?,?,?,?)",
        Statement.RETURN_GENERATED_KEYS);

      ps.setInt(1, itemId);

      ps.setInt(2, userId);

      ps.setDouble(3, winnerAmount);

      ps.setDouble(4, total);

      ps.setInt(5, expedited?1:0);

      if (closesAt != null) ps.setString(6, closesAt); else ps.setNull(6, java.sql.Types.VARCHAR);

      return ps;

    }, kh);
    return kh.getKey() == null ? -1L : kh.getKey().longValue();
    
  }
}

