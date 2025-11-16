package com.example.auth.config;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
  private final AuthService auth;
  private final Environment env;

  public DataInitializer(AuthService auth, Environment env) {
    this.auth = auth;
    this.env = env;
  }

  @Override
  public void run(ApplicationArguments args) {
    
    seedCustomFromProperties();

    
    seedUser("alice", false, "Alice", "Smith", "100 King St", "10", "Toronto", "CA", "A1A1A1", "Password123!");
    seedUser("bob", true,  "Bob",   "Jones", "200 Queen St", "20", "Toronto", "CA", "B2B2B2", "Password123!");
  }

  private void seedCustomFromProperties() {
    String username = trimToNull(env.getProperty("app.user.username"));

    if (username == null) return; 
    boolean enable2fa = Boolean.parseBoolean(env.getProperty("app.user.enable2fa", "false"));

    String password = valueOr(env.getProperty("app.user.password"), "Password123!");

    String first = valueOr(env.getProperty("app.user.firstName"), "Demo");

    String last = valueOr(env.getProperty("app.user.lastName"), "User");

    String streetName = valueOr(env.getProperty("app.user.streetName"), "Main St");

    String streetNumber = valueOr(env.getProperty("app.user.streetNumber"), "1");

    String city = valueOr(env.getProperty("app.user.city"), "Toronto");

    String country = valueOr(env.getProperty("app.user.country"), "CA");

    String postal = valueOr(env.getProperty("app.user.postalCode"), "A1A1A1");

    seedUser(username.toLowerCase(), enable2fa, first, last, streetName, streetNumber, city, country, postal, password);
  }

  private void seedUser(String username, boolean enable2fa, String first, String last, String streetName, String streetNumber, String city, String country, String postal, String password) {
    auth.findByUsername(username).ifPresentOrElse(u -> {
      
    }, () -> {
      User created = auth.register(username, password, first, last, streetName, streetNumber, city, country, postal, enable2fa);
      if (enable2fa && created.getTwofaSecret() != null) {
        log.info("User '{}' seeded with 2FA. Add to authenticator using secret: {}", username, created.getTwofaSecret());
      } else {
        log.info("User '{}' seeded.", username);
      }
    });
  }

  private static String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  private static String valueOr(String s, String def) {
    String t = trimToNull(s);
    return t == null ? def : t;
  }
}

