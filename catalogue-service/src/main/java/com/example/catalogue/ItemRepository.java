package com.example.catalogue;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ItemRepository {
  private final JdbcTemplate jdbc;
  public ItemRepository(JdbcTemplate jdbc){ 
    this.jdbc = jdbc; 
  }

  private static final RowMapper<Item> MAPPER = (rs,i) -> {
    Item it = new Item();
    it.id = rs.getInt("id");
    it.name = rs.getString("name");
    it.description = rs.getString("description");
    it.status = rs.getString("status");
    it.startingPrice = rs.getDouble("starting_price");
    it.currentPrice = rs.getDouble("current_price");
    it.auctionType = rs.getString("auction_type");
    it.closesAt = rs.getString("closes_at");
    try { 
      it.shippingPrice = rs.getDouble("shipping_price"); 
    } catch(Exception ignored){}
    try { 
      it.expeditePrice = rs.getDouble("expedite_price"); 
    } catch(Exception ignored){}
    try { 
      it.shippingDays = rs.getInt("shipping_days"); 
    } catch(Exception ignored){}
    try { 
      it.sellerUserId = rs.getInt("seller_user_id"); 
    } catch(Exception ignored){}
    return it;
  };

  public List<Item> listActive(String q){
    String like = "%" + (q==null?"":q.trim()) + "%";
    return jdbc.query("SELECT * FROM items WHERE status IN ('UPCOMING','ACTIVE') AND (name LIKE ? OR description LIKE ?) ORDER BY id DESC", MAPPER, like, like);
  }

  public Optional<Item> find(int id){
    var list = jdbc.query("SELECT * FROM items WHERE id=?", MAPPER, id);
    return list.stream().findFirst();
  }

  public int create(Item it){
    return jdbc.update("INSERT INTO items(name,description,status,starting_price,current_price,auction_type,closes_at,shipping_price,expedite_price,shipping_days,seller_user_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
      it.name, it.description, it.status, it.startingPrice, it.currentPrice, it.auctionType, it.closesAt, it.shippingPrice, it.expeditePrice, it.shippingDays, it.sellerUserId);
  }

  public int updateStatus(int id, String status){
    return jdbc.update("UPDATE items SET status=? WHERE id=?", status, id);
  }

  public int updateDutchPrice(int id, double newPrice){
    return jdbc.update("UPDATE items SET current_price=? WHERE id=? AND auction_type='DUTCH' AND status IN ('UPCOMING','ACTIVE') AND ? < current_price", newPrice, id, newPrice);
  }

  public int delete(int id){
    return jdbc.update("DELETE FROM items WHERE id=?", id);
  }
}
