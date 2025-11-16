package com.example.auth.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ItemsInitializer implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public ItemsInitializer(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='items'",
        Integer.class);
    if (count == null || count == 0)
      return; 
    
    try {
      jdbc.execute("ALTER TABLE items ADD COLUMN shipping_days INTEGER DEFAULT 5");
    } catch (Exception ignored) {
    }
    try {
      jdbc.execute("ALTER TABLE items ADD COLUMN seller_user_id INTEGER");
    } catch (Exception ignored) {
    }

    
    try {
      jdbc.update("UPDATE items SET shipping_days = COALESCE(shipping_days, 5)");
    } catch (Exception ignored) {
    }

    Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM items", Integer.class);
    if (n != null && n > 0)
      return; 

    java.time.format.DateTimeFormatter FMT = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    java.time.ZonedDateTime nowUtc = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC);
    String plus2h = nowUtc.plusHours(2).format(FMT);
    String plus50m = nowUtc.plusMinutes(50).format(FMT);
    String minus1d = nowUtc.minusDays(1).format(FMT);

    
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Vintage Camera", "A 1970s film camera in working condition", "ACTIVE", 80.0, 95.0, "FORWARD", plus2h, 15.0,
        25.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Gaming Laptop", "15-inch, i7 CPU, GTX GPU, lightly used", "UPCOMING", 500.0, 500.0, "DUTCH", null, 30.0, 40.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Mountain Bike", "Hardtail, medium frame, good tires", "ACTIVE", 150.0, 175.0, "FORWARD", plus50m, 20.0, 35.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Smartphone", "Unlocked, 128GB storage", "CLOSED", 120.0, 140.0, "FORWARD", minus1d, 10.0, 20.0);

    
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "4K TV", "55-inch UHD Smart TV", "ACTIVE", 700.0, 700.0, "DUTCH", null, 45.0, 60.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Noise Cancelling Headphones", "Over-ear wireless ANC", "UPCOMING", 180.0, 180.0, "DUTCH", null, 12.0, 18.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Office Chair", "Ergonomic mesh with lumbar support", "ACTIVE", 220.0, 220.0, "DUTCH", null, 25.0, 35.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Tablet", "10-inch tablet, 128GB", "UPCOMING", 260.0, 260.0, "DUTCH", null, 15.0, 25.0);
    jdbc.update(
        "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price) VALUES (?,?,?,?,?,?,?,?,?)",
        "Electric Scooter", "Foldable commuter scooter", "ACTIVE", 350.0, 350.0, "DUTCH", null, 30.0, 45.0);

    
    try {
      jdbc.update("UPDATE items SET shipping_days=7 WHERE name='Vintage Camera'");
      jdbc.update("UPDATE items SET shipping_days=3 WHERE name='Gaming Laptop'");
      jdbc.update("UPDATE items SET shipping_days=5 WHERE name='Mountain Bike'");
      jdbc.update("UPDATE items SET shipping_days=4 WHERE name='4K TV'");
      jdbc.update("UPDATE items SET shipping_days=2 WHERE name='Noise Cancelling Headphones'");
      jdbc.update("UPDATE items SET shipping_days=6 WHERE name='Office Chair'");
      jdbc.update("UPDATE items SET shipping_days=3 WHERE name='Tablet'");
      jdbc.update("UPDATE items SET shipping_days=5 WHERE name='Electric Scooter'");
    } catch (Exception ignored) {
    }
  }
}

