package com.example.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc){ 
    this.jdbc = jdbc; 
  }

  private static final RowMapper<User> MAPPER = (rs,i) -> {
    User u = new User();

    u.id = rs.getInt("id");
    
    u.username = rs.getString("username");

    u.passwordHash = rs.getString("password_hash");

    u.firstName = rs.getString("first_name");

    u.lastName = rs.getString("last_name");

    u.streetName = rs.getString("street_name");

    u.streetNumber = rs.getString("street_number");

    u.city = rs.getString("city");

    u.country = rs.getString("country");

    u.postalCode = rs.getString("postal_code");

    u.twofaSecret = rs.getString("twofa_secret");
    
    return u;
  };

  public Optional<User> findByUsername(String username){
    List<User> list = jdbc.query("SELECT * FROM users WHERE username=?", MAPPER, username);
    return list.stream().findFirst();
  }

  public Optional<User> findById(int id){
    List<User> list = jdbc.query("SELECT * FROM users WHERE id=?", MAPPER, id);
    return list.stream().findFirst();
  }

  public int create(User u){
    return jdbc.update("INSERT INTO users(username,password_hash,first_name,last_name,street_name,street_number,city,country,postal_code,twofa_secret) VALUES (?,?,?,?,?,?,?,?,?,?)",
      u.username, u.passwordHash, u.firstName, u.lastName, u.streetName, u.streetNumber, u.city, u.country, u.postalCode, u.twofaSecret);
  }
}

