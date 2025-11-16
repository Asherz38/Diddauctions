package com.example.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class PaymentController {
  private final PaymentServer server;
  public PaymentController(PaymentServer server){ this.server = server; 
  }

  @PostMapping("/api/payments")
  public ResponseEntity<?> pay(@RequestBody Map<String,Object> body){
    try {
      int itemId = Integer.parseInt(String.valueOf(body.get("itemId")));
      Integer userId = body.get("userId") == null ? null : Integer.parseInt(String.valueOf(body.get("userId")));
      boolean expedite = Boolean.parseBoolean(String.valueOf(body.getOrDefault("expedite", false)));
      return ResponseEntity.ok(server.pay(itemId, userId, expedite));
    } catch (IllegalArgumentException | IllegalStateException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }
}

