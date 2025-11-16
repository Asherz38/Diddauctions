package com.example.auction;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {
  private final AuctionServer server;

  public AuctionController(AuctionServer server) {
    this.server = server;
  }

  @PostMapping("/{id}/bids")
  public ResponseEntity<?> placeBid(@PathVariable int id, @RequestBody Map<String, Object> body) {
    double amount = Double.parseDouble(String.valueOf(body.get("amount")));

    int userId = Integer.parseInt(String.valueOf(body.get("userId")));

    try {
      return ResponseEntity.ok(server.placeBid(id, userId, amount));

    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));

    } catch (IllegalStateException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/{id}/buy-now")
  public ResponseEntity<?> buyNow(@PathVariable int id, @RequestBody Map<String, Object> body) {
    int userId = Integer.parseInt(String.valueOf(body.get("userId")));

    try {
      return ResponseEntity.ok(server.buyNow(id, userId));

    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));

    } catch (IllegalStateException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
      
    }
  }

  @GetMapping("/{id}/top")
  public ResponseEntity<?> top(@PathVariable int id) {
    return server.topBid(id).<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok(Map.of()));
  }

  @PostMapping("/close-expired")
  public ResponseEntity<?> closeExpired() {
    int n = server.closeExpired();
    return ResponseEntity.ok(Map.of("closed", n));
  }
}
