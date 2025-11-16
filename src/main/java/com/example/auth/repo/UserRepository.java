package com.example.auth.repo;

import com.example.auth.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final RowMapper<User> MAPPER = (rs, i) -> {
    User u = new User();

    u.setId(rs.getInt("id"));

    u.setUsername(rs.getString("username"));

    u.setPasswordHash(rs.getString("password_hash"));

    u.setFirstName(rs.getString("first_name"));

    u.setLastName(rs.getString("last_name"));

    u.setStreetName(rs.getString("street_name"));

    u.setStreetNumber(rs.getString("street_number"));

    u.setCity(rs.getString("city"));

    u.setCountry(rs.getString("country"));

    u.setPostalCode(rs.getString("postal_code"));

    u.setTwofaSecret(rs.getString("twofa_secret"));

    return u;
  };

  public Optional<User> findByUsername(String username) {
    List<User> list = jdbc.query(         "SELECT id, username, password_hash, first_name, last_name, street_name, street_number, city, country, postal_code, twofa_secret FROM users WHERE username = ?",
        MAPPER, username);

    return list.stream().findFirst();
  }

  public void create(String username, String passwordHash,
  String firstName, String lastName, String streetName, String streetNumber, String city, String country, String postalCode, String twofaSecret) throws DataAccessException {
    jdbc.update(
 "INSERT INTO users(username, password_hash, first_name, last_name, street_name, street_number, city, country, postal_code, twofa_secret) VALUES (?,?,?,?,?,?,?,?,?,?)", username, passwordHash, firstName, lastName, streetName, streetNumber, city, country, postalCode, twofaSecret);
  }

  public void updatePassword(String username, String newPasswordHash) throws DataAccessException {
    jdbc.update("UPDATE users SET password_hash = ? WHERE username = ?", newPasswordHash, username);
  }
}


