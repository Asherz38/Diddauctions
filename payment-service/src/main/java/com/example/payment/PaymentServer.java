package com.example.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class PaymentServer {
  private final PaymentRepository repo;

  private final RestTemplate http = new RestTemplate();

  @Value("${catalogue.api.base-url}") private String catalogueApi;

  @Value("${auction.api.base-url}") private String auctionApi;

  public PaymentServer(PaymentRepository repo){ 
    this.repo = repo; 
  }

  public Map<String,Object> pay(int itemId, Integer userId, boolean expedite){
    
    Map<String,Object> item = http.exchange(
      catalogueApi+"/api/items/"+itemId,
      HttpMethod.GET,
      null,
      new ParameterizedTypeReference<Map<String,Object>>(){}
    ).getBody();
    if (item == null) throw new IllegalArgumentException("item not found");

    
    Map<String,Object> top = http.exchange(
      auctionApi+"/api/auctions/"+itemId+"/top",
      HttpMethod.GET,
      null,
      new ParameterizedTypeReference<Map<String,Object>>(){}
    ).getBody();

    if (top == null || !top.containsKey("amount")) throw new IllegalStateException("no winner");

    int winnerUserId = ((Number) top.get("userId")).intValue();

    double winnerAmount = ((Number) top.get("amount")).doubleValue();

    if (userId != null && userId != winnerUserId) throw new IllegalArgumentException("user is not winner");

    double shipping = item.get("shippingPrice") == null ? 0d : ((Number)item.get("shippingPrice")).doubleValue();

    double expeditePrice = item.get("expeditePrice") == null ? 0d : ((Number)item.get("expeditePrice")).doubleValue();

    String closesAt = item.get("closesAt") == null ? null : String.valueOf(item.get("closesAt"));

    double total = winnerAmount + shipping + (expedite ? expeditePrice : 0d);

    long receiptId = repo.record(itemId, winnerUserId, winnerAmount, total, expedite, closesAt);

    
    try { 
      http.delete(catalogueApi+"/api/items/"+itemId); 
    } catch(Exception ignored) {}

    return Map.of("receiptId", receiptId, "total", total, "winnerUserId", winnerUserId, "winnerAmount", winnerAmount);
  }
}


