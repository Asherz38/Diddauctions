package com.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/controller")
public class FacadeController {
  private final ControllerServer server;
  public FacadeController(ControllerServer server){ this.server = server; 
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody Map<String,String> body){
    try { return ResponseEntity.ok(server.login(body.get("username"), body.get("password"))); }
    catch (IllegalArgumentException ex){ 
      return ResponseEntity.status(401).body(Map.of("error", ex.getMessage())); 
    }
  }

  @PostMapping("/forward-bid")
  public ResponseEntity<?> forwardBid(@RequestBody Map<String,Object> body){
    try {
      String token = String.valueOf(body.get("token"));
      int itemId = Integer.parseInt(String.valueOf(body.get("itemId")));
      double amount = Double.parseDouble(String.valueOf(body.get("amount")));
      return ResponseEntity.ok(server.forwardBid(token, itemId, amount));
    } catch (RuntimeException ex){ 
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); 
    }
  }

  @PostMapping("/dutch-buy")
  public ResponseEntity<?> dutchBuy(@RequestBody Map<String,Object> body){
    try {
      String token = String.valueOf(body.get("token"));
      int itemId = Integer.parseInt(String.valueOf(body.get("itemId")));
      return ResponseEntity.ok(server.dutchBuy(token, itemId));
    } catch (RuntimeException ex){ 
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); 
    }
  }

  @PostMapping("/pay")
  public ResponseEntity<?> pay(@RequestBody Map<String,Object> body){
    try {
      String token = String.valueOf(body.get("token"));
      int itemId = Integer.parseInt(String.valueOf(body.get("itemId")));
      boolean expedite = Boolean.parseBoolean(String.valueOf(body.getOrDefault("expedite", false)));
      return ResponseEntity.ok(server.pay(token, itemId, expedite));
    } catch (RuntimeException ex){ 
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); 
    }
  }

  @GetMapping("/item/{id}")
  public ResponseEntity<?> aggregate(@PathVariable int id){
    return ResponseEntity.ok(server.aggregateItem(id));
  }

  @GetMapping("/catalog")
  public ResponseEntity<?> catalog(@RequestParam(name="q", required=false) String q){
    return ResponseEntity.ok(server.listCatalog(q));
  }

  @GetMapping("/bid-state")
  public ResponseEntity<?> bidState(@RequestParam("id") int id){
    return ResponseEntity.ok(server.bidState(id));
  }

  @PostMapping("/sell")
  public ResponseEntity<?> sell(
      @RequestHeader("X-Session-Token") String token,
      @RequestBody java.util.Map<String,Object> item,
      @RequestParam(name="duration", required=false) Integer duration,
      @RequestParam(name="unit", required=false) String unit
  ){
    try { 
      return ResponseEntity.ok(server.createItem(token, item, duration, unit)); 
    } catch (RuntimeException ex){
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @GetMapping("/sell/manage")
  public ResponseEntity<?> sellManage(@RequestHeader("X-Session-Token") String token){
    return ResponseEntity.ok(server.listSellerDutch(token));
  }

  @PostMapping("/sell/dutch/price")
  public ResponseEntity<?> updateDutchPrice(
      @RequestHeader("X-Session-Token") String token,
      @RequestParam("id") int id,
      @RequestParam("newPrice") double newPrice
  ){
    try { 
      return ResponseEntity.ok(server.updateDutchPrice(token, id, newPrice)); 
    } catch (RuntimeException ex){
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Map<String,Object> body){
    try {
      String username = String.valueOf(body.get("username"));
      String password = String.valueOf(body.get("password"));
      java.util.Map<String,String> profile = new java.util.HashMap<>();
      for (var k : java.util.List.of("firstName","lastName","streetName","streetNumber","city","country","postalCode")){
        if (body.containsKey(k) && body.get(k) != null) profile.put(k, String.valueOf(body.get(k)));
      }
      return ResponseEntity.ok(server.register(username, password, profile));
    } catch (RuntimeException ex){
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(@RequestHeader("X-Session-Token") String token){
    return ResponseEntity.ok(server.me(token));
  }
}

