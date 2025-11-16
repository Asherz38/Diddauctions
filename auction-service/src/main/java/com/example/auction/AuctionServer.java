package com.example.auction;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuctionServer {
  private final BidRepository bids;
  private final JdbcTemplate jdbc;
  private final RestTemplate http = new RestTemplate();
  @Value("${catalogue.api.base-url}") 
  private String catalogueApi;

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public AuctionServer(BidRepository bids, JdbcTemplate jdbc){ 
    this.bids = bids; this.jdbc = jdbc; 
  }

  public Map<String,Object> fetchItem(int id){
    return http.exchange(catalogueApi+"/api/items/"+id, HttpMethod.GET, null,
      new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();
  }

  public boolean isClosed(Map<String,Object> item){
    String status = String.valueOf(item.get("status"));

    if ("CLOSED".equals(status)) 
    return true;
    String type = String.valueOf(item.get("auctionType"));

    if ("FORWARD".equalsIgnoreCase(type)){
      Object c = item.get("closesAt");
      if (c != null){
        try{
          LocalDateTime end = LocalDateTime.parse(String.valueOf(c), FMT);
          return end.isBefore(LocalDateTime.now(ZoneOffset.UTC));
        }
        catch(Exception ignored){}
      }
    }
    return false;
  }

  public Map<String,Object> placeBid(int itemId, int userId, double amount){
    Map<String,Object> item = fetchItem(itemId);
    if (item == null) 
    throw new IllegalArgumentException("item not found");
    
    if (!"FORWARD".equals(String.valueOf(item.get("auctionType")))) 
    throw new IllegalArgumentException("not forward");

    if (isClosed(item)) 
    throw new IllegalStateException("closed");
    double current = item.get("currentPrice") == null ? ((Number)item.get("startingPrice")).doubleValue() : ((Number)item.get("currentPrice")).doubleValue();
    
    if (amount <= current) throw new IllegalArgumentException("must be > current");
    
    
    Object ca = item.get("closesAt");
    if (ca != null){
      jdbc.update("INSERT OR IGNORE INTO auctions(item_id, closes_at) VALUES (?,?)", itemId, String.valueOf(ca));
    }
    bids.placeBid(itemId, userId, amount);
    return Map.of("ok", true);
  }

  public Map<String,Object> buyNow(int itemId, int userId){
    Map<String,Object> item = fetchItem(itemId);
    if (item == null) throw new IllegalArgumentException("item not found");

    if (!"DUTCH".equals(String.valueOf(item.get("auctionType")))) throw new IllegalArgumentException("not dutch");

    if (isClosed(item)) throw new IllegalStateException("closed");

    double price = ((Number)item.get("currentPrice")).doubleValue();
    bids.placeBid(itemId, userId, price);

    
    jdbc.update("INSERT INTO wins(item_id,user_id,amount,ended_at) VALUES (?,?,?,datetime('now'))", itemId, userId, price);

    
    http.delete(catalogueApi+"/api/items/"+itemId);

    
    jdbc.update("DELETE FROM auctions WHERE item_id=?", itemId);
    return Map.of("ok", true);
  }

  public Optional<Map<String,Object>> topBid(int itemId){
    return bids.topBid(itemId);
  }

  public int closeExpired(){
    
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    List<Map<String,Object>> expired = jdbc.query("SELECT item_id, closes_at FROM auctions",
      (rs,i) -> Map.of("itemId", rs.getInt(1), "closesAt", rs.getString(2)));
    int closed = 0;
    for (var row : expired){
      LocalDateTime end;
      try{ 
        end = LocalDateTime.parse(String.valueOf(row.get("closesAt")), FMT);
      }catch(Exception e){ 
        continue; 
      }
      if (end.isAfter(now)) continue;
      int itemId = ((Number)row.get("itemId")).intValue();
      var top = bids.topBid(itemId);
      if (top.isPresent()){
        int winner = ((Number)top.get().get("userId")).intValue();
        double amount = ((Number)top.get().get("amount")).doubleValue();
        jdbc.update("INSERT INTO wins(item_id,user_id,amount,ended_at) VALUES (?,?,?,datetime('now'))", itemId, winner, amount);
      }
      
      try{ http.delete(catalogueApi+"/api/items/"+itemId); } catch(Exception ignored) {}
      jdbc.update("DELETE FROM auctions WHERE item_id=?", itemId);
      closed++;
    }
    return closed;
  }
}