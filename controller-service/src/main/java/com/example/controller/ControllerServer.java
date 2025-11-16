package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class ControllerServer {
  private final SessionRepository sessions;
  private final RestTemplate http = new RestTemplate();
  @Value("${identity.api.base-url}") private String identityApi;
  @Value("${catalogue.api.base-url}") private String catalogueApi;
  @Value("${auction.api.base-url}") private String auctionApi;
  @Value("${payment.api.base-url}") private String paymentApi;

  public ControllerServer(SessionRepository sessions){ 
    this.sessions = sessions; 
  }

  public Map<String,Object> login(String username, String password){
    java.util.Map<String,Object> req = new java.util.HashMap<>();
    req.put("username", username);
    req.put("password", password);
    var resp = http.exchange(
      identityApi+"/auth/login",
      HttpMethod.POST,
      new HttpEntity<>(req),
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    if (!resp.getStatusCode().is2xxSuccessful()) throw new IllegalArgumentException("invalid credentials");

    Map<String,Object> body = resp.getBody();

    int userId = ((Number) body.get("userId")).intValue();

    String token = UUID.randomUUID().toString();

    sessions.create(token, userId, username.toLowerCase());

    sessions.recordEvent("login", null, userId, username);

    return java.util.Map.of("token", token, "userId", userId, "username", username);
  }

  private Map<String,Object> requireSession(String token){
    return sessions.findByToken(token).orElseThrow(() -> new IllegalArgumentException("invalid session"));
  }

  public Map<String,Object> forwardBid(String token, int itemId, double amount){
    var sess = requireSession(token);

    int userId = ((Number) sess.get("userId")).intValue();

    java.util.Map<String,Object> payload = new java.util.HashMap<>();

    payload.put("userId", userId);

    payload.put("amount", amount);
    var res = http.exchange(
      auctionApi+"/api/auctions/"+itemId+"/bids",
      HttpMethod.POST,
      new HttpEntity<>(payload),
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    sessions.recordEvent("bid", itemId, userId, String.valueOf(amount));

    return res.getBody()==null? java.util.Map.of("ok",true):res.getBody();
  }

  public Map<String,Object> dutchBuy(String token, int itemId){
    var sess = requireSession(token);

    int userId = ((Number) sess.get("userId")).intValue();

    java.util.Map<String,Object> payload = new java.util.HashMap<>();

    payload.put("userId", userId);

    var res = http.exchange(
      auctionApi+"/api/auctions/"+itemId+"/buy-now",
      HttpMethod.POST,
      new HttpEntity<>(payload),
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    sessions.recordEvent("buy-now", itemId, userId, null);

    return res.getBody()==null? java.util.Map.of("ok",true):res.getBody();
  }

  public Map<String,Object> pay(String token, int itemId, boolean expedite){
    var sess = requireSession(token);

    int userId = ((Number) sess.get("userId")).intValue();

    java.util.Map<String,Object> payload = new java.util.HashMap<>();

    payload.put("itemId", itemId);

    payload.put("userId", userId);

    payload.put("expedite", expedite);

    var res = http.exchange(
      paymentApi+"/api/payments",
      HttpMethod.POST,
      new HttpEntity<>(payload),
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    sessions.recordEvent("pay", itemId, userId, String.valueOf(expedite));

    return res.getBody()==null? java.util.Map.of("ok",true):res.getBody();
  }

  public Map<String,Object> aggregateItem(int id){
    Map<String,Object> item = http.exchange(catalogueApi+"/api/items/"+id, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();

    Map<String,Object> top = http.exchange(auctionApi+"/api/auctions/"+id+"/top", HttpMethod.GET, null, new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();
    
    return java.util.Map.of("item", item, "topBid", top);
  }

  
  public java.util.List<Map<String,Object>> listCatalog(String q){
    var resp = http.exchange(
      catalogueApi+"/api/items" + (q!=null && !q.isBlank()? ("?q="+urlEncode(q)) : ""),
      HttpMethod.GET,
      null,
      new ParameterizedTypeReference<java.util.List<Map<String,Object>>>(){}
    );
    return resp.getBody() == null ? java.util.List.of() : resp.getBody();
  }

  public Map<String,Object> bidState(int id){
    Map<String,Object> item = http.exchange(catalogueApi+"/api/items/"+id, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();
    java.util.Map<String,Object> map = new java.util.HashMap<>();
    if (item == null) return map;
    map.put("status", item.get("status"));
    map.put("currentPrice", item.get("currentPrice") == null ? item.get("startingPrice") : item.get("currentPrice"));
    
    Object type = item.get("auctionType");
    Object closesAt = item.get("closesAt");
    if (type != null && "FORWARD".equalsIgnoreCase(String.valueOf(type)) && closesAt != null){
      try{
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(String.valueOf(closesAt), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long secs = java.time.Duration.between(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC), end).getSeconds();
        map.put("remainingSeconds", Math.max(0, secs));
      }catch(Exception ignored){}
    }
    Map<String,Object> top = http.exchange(auctionApi+"/api/auctions/"+id+"/top", HttpMethod.GET, null, new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();
    if (top != null && top.containsKey("amount")){
      map.put("topAmount", top.get("amount"));
      map.put("topUser", top.get("username"));
    }
    return map;
  }

  public Map<String,Object> createItem(String token, Map<String,Object> itemBody, Integer duration, String unit){
    var sess = requireSession(token);
    int userId = ((Number) sess.get("userId")).intValue();
    itemBody.put("sellerUserId", userId);
    var entity = new HttpEntity<>(itemBody);
    var resp = http.exchange(
      catalogueApi+"/api/items" + buildDurationParams(duration, unit),
      HttpMethod.POST,
      entity,
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    return resp.getBody() == null ? java.util.Map.of("ok", true) : resp.getBody();
  }

  public java.util.List<Map<String,Object>> listSellerDutch(String token){
    var sess = requireSession(token);
    int userId = ((Number) sess.get("userId")).intValue();
    var list = listCatalog(null);
    java.util.List<Map<String,Object>> out = new java.util.ArrayList<>();
    for (var it : list){
      if ("DUTCH".equals(String.valueOf(it.get("auctionType"))) && it.get("sellerUserId") != null){
        int seller = ((Number)it.get("sellerUserId")).intValue();
        if (seller == userId) out.add(it);
      }
    }
    return out;
  }

  public Map<String,Object> updateDutchPrice(String token, int id, double newPrice){
    var sess = requireSession(token);
    int userId = ((Number) sess.get("userId")).intValue();
    Map<String,Object> item = http.exchange(catalogueApi+"/api/items/"+id, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String,Object>>(){}).getBody();
    if (item == null) throw new IllegalArgumentException("item not found");
    int seller = item.get("sellerUserId") == null ? -1 : ((Number)item.get("sellerUserId")).intValue();
    if (seller != userId) throw new IllegalArgumentException("not owner");
    var resp = http.exchange(
      catalogueApi+"/api/items/"+id+"/price?newPrice="+newPrice,
      HttpMethod.PATCH,
      null,
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    return resp.getBody() == null ? java.util.Map.of("ok", true) : resp.getBody();
  }

  public Map<String,Object> register(String username, String password, Map<String,String> profile){
    java.util.Map<String,String> req = new java.util.HashMap<>();
    req.put("username", username);
    req.put("password", password);
    if (profile != null) req.putAll(profile);
    var resp = http.exchange(
      identityApi+"/auth/signup",
      HttpMethod.POST,
      new HttpEntity<>(req),
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    return resp.getBody() == null ? java.util.Map.of("ok", true) : resp.getBody();
  }

  public Map<String,Object> me(String token){
    var sess = requireSession(token);
    String username = String.valueOf(sess.get("username"));
    var resp = http.exchange(
      identityApi+"/users/by-username/"+urlEncode(username),
      HttpMethod.GET,
      null,
      new ParameterizedTypeReference<Map<String,Object>>(){}
    );
    return resp.getBody() == null ? java.util.Map.of() : resp.getBody();
  }

  private static String buildDurationParams(Integer duration, String unit){
    StringBuilder sb = new StringBuilder();
    if (duration != null){
      sb.append("?duration=").append(duration);
      if (unit != null && !unit.isBlank()) sb.append("&unit=").append(urlEncode(unit));
    }
    return sb.toString();
  }

  private static String urlEncode(String s){
    try{ return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8); }
    catch(Exception e){ return s; }
  }
}

