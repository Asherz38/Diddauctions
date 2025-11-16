package com.example.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class SessionRepository {
  private final JdbcTemplate jdbc;
  public SessionRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; 
  }

  public void create(String token, int userId, String username){
    jdbc.update("INSERT INTO sessions(token,user_id,username) VALUES (?,?,?)", token, userId, username);
  }

  public Optional<Map<String,Object>> findByToken(String token){
    var list = jdbc.query("SELECT token,user_id,username,created_at FROM sessions WHERE token=?", (rs,i) -> {
      java.util.Map<String,Object> m = new java.util.HashMap<>();
      m.put("token", rs.getString(1));
      m.put("userId", rs.getInt(2));
      m.put("username", rs.getString(3));
      m.put("createdAt", rs.getString(4));
      return m;
    }, token);
    return list.stream().findFirst();
  }

  public void recordEvent(String type, Integer itemId, Integer userId, String data){
    jdbc.update("INSERT INTO events(type,item_id,user_id,data) VALUES (?,?,?,?)", type, itemId, userId, data);
  }
}

