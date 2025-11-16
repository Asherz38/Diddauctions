package com.example.auth.repo;

import com.example.auth.model.Item;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ItemRepository {
  private final JdbcTemplate jdbc;

  public ItemRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  private static final RowMapper<Item> MAPPER = (rs, i) -> {
    Item it = new Item();

    it.setId(rs.getInt("id"));

    it.setName(rs.getString("name"));

    it.setDescription(rs.getString("description"));

    it.setStatus(rs.getString("status"));

    it.setStartingPrice(rs.getDouble("starting_price"));

    try {
      it.setCurrentPrice(rs.getDouble("current_price"));
    } catch (Exception ignored) {
    }
    try {
      it.setAuctionType(rs.getString("auction_type"));
    } catch (Exception ignored) {
    }
    try {
      it.setClosesAt(rs.getString("closes_at"));
    } catch (Exception ignored) {
    }
    try {
      it.setShippingPrice(rs.getDouble("shipping_price"));
    } catch (Exception ignored) {
    }
    try {
      it.setExpeditePrice(rs.getDouble("expedite_price"));
    } catch (Exception ignored) {
    }
    try {
      it.setShippingDays(rs.getInt("shipping_days"));
    } catch (Exception ignored) {
    }
    try {
      int s = rs.getInt("seller_user_id");

      if (!rs.wasNull())
        it.setSellerUserId(s);
    } catch (Exception ignored) {
    }
    try {
      long s = rs.getLong("remaining_seconds");

      if (rs.wasNull())
        it.setRemainingSeconds(null);
      else
        it.setRemainingSeconds(Math.max(0, s));

    } catch (Exception ignored) {
    }
    return it;
  };

  public List<Item> search(String keyword) {
    String like = "%" + (keyword == null ? "" : keyword.trim()) + "%";
    String sql = "SELECT id, name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price, shipping_days, "
        + "CASE WHEN auction_type='FORWARD' AND closes_at IS NOT NULL THEN (strftime('%s', closes_at) - strftime('%s','now')) END AS remaining_seconds "
        + "FROM items WHERE status IN ('UPCOMING','ACTIVE') AND (name LIKE ? OR description LIKE ?) "
        + "ORDER BY status='ACTIVE' DESC, id DESC";
    return jdbc.query(sql, MAPPER, like, like);
  }

  public List<Item> listActive() {
    String sql = "SELECT id, name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price, shipping_days, "
        + "CASE WHEN auction_type='FORWARD' AND closes_at IS NOT NULL THEN (strftime('%s', closes_at) - strftime('%s','now')) END AS remaining_seconds "
        + "FROM items WHERE status IN ('UPCOMING','ACTIVE') ORDER BY status='ACTIVE' DESC, id DESC";
    return jdbc.query(sql, MAPPER);
  }

  public Item findById(int id) {
    String sql = "SELECT id, name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price, shipping_days, "
        + "CASE WHEN auction_type='FORWARD' AND closes_at IS NOT NULL THEN (strftime('%s', closes_at) - strftime('%s','now')) END AS remaining_seconds "
        + "FROM items WHERE id = ?";
    return jdbc.query(sql, MAPPER, id).stream().findFirst().orElse(null);
  }

  public int closeExpired() {
    String sql = "UPDATE items SET status='CLOSED' \n" + "WHERE auction_type='FORWARD' AND closes_at IS NOT NULL \n"
        + "AND status != 'CLOSED' AND datetime(closes_at) <= datetime('now')";
    return jdbc.update(sql);
  }

  public long create(String name, String description, String auctionType,
      double startingPrice, Double currentPrice,
      String status, String closesAt,
      Double shippingPrice, Double expeditePrice, Integer shippingDays,
      Integer sellerUserId) {
    var kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbc.update(con -> {
      var ps = con.prepareStatement(
          "INSERT INTO items(name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price, shipping_days, seller_user_id) "
              + "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
          java.sql.Statement.RETURN_GENERATED_KEYS);

      ps.setString(1, name);

      ps.setString(2, description);

      ps.setString(3, status);

      ps.setDouble(4, startingPrice);

      ps.setDouble(5, currentPrice != null ? currentPrice : startingPrice);

      ps.setString(6, auctionType);

      if (closesAt != null)
        ps.setString(7, closesAt);
      else
        ps.setNull(7, java.sql.Types.VARCHAR);

      if (shippingPrice != null)
        ps.setDouble(8, shippingPrice);
      else
        ps.setNull(8, java.sql.Types.REAL);

      if (expeditePrice != null)
        ps.setDouble(9, expeditePrice);
      else
        ps.setNull(9, java.sql.Types.REAL);

      if (shippingDays != null)
        ps.setInt(10, shippingDays);
      else
        ps.setNull(10, java.sql.Types.INTEGER);

      if (sellerUserId != null)
        ps.setInt(11, sellerUserId);
      else
        ps.setNull(11, java.sql.Types.INTEGER);

      return ps;
    }, kh);
    Number key = kh.getKey();
    return key == null ? -1L : key.longValue();
  }

  public List<Item> listDutchBySeller(int sellerId) {
    String sql = "SELECT id, name, description, status, starting_price, current_price, auction_type, closes_at, shipping_price, expedite_price, shipping_days, seller_user_id, "
        + "CASE WHEN auction_type='FORWARD' AND closes_at IS NOT NULL THEN (strftime('%s', closes_at) - strftime('%s','now')) END AS remaining_seconds "
        + "FROM items WHERE auction_type='DUTCH' AND status IN ('UPCOMING','ACTIVE') AND seller_user_id = ? ORDER BY id DESC";
    return jdbc.query(sql, MAPPER, sellerId);
  }

  public int updateDutchPrice(int id, int sellerId, double newPrice) {
    String sql = "UPDATE items SET current_price=? WHERE id=? AND seller_user_id=? AND auction_type='DUTCH' AND status IN ('UPCOMING','ACTIVE') AND ? < current_price AND ? >= 0";
    return jdbc.update(sql, newPrice, id, sellerId, newPrice, newPrice);
  }
}


