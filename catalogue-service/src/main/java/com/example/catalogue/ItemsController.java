package com.example.catalogue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemsController {
  private final ItemRepository repo;
  public ItemsController(ItemRepository repo){ this.repo = repo; }

  @GetMapping
  public List<Item> list(@RequestParam(name="q", required=false) String q){
    return repo.listActive(q);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Item> get(@PathVariable int id){
    return repo.find(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody Item it,
@RequestParam(name="duration", required=false) Integer duration, @RequestParam(name="unit", required=false) String unit){
    if (it.name == null || it.name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error","name required"));
    if (it.auctionType == null) it.auctionType = "FORWARD";
    if (it.startingPrice == null) it.startingPrice = 0d;
    if (it.currentPrice == null) it.currentPrice = it.startingPrice;
    it.status = "ACTIVE";
    if ("FORWARD".equalsIgnoreCase(it.auctionType)){
      if (duration == null) return ResponseEntity.badRequest().body(Map.of("error","duration required for forward"));
      long minutes = switch (unit == null ? "hours" : unit.toLowerCase()){
        case "minutes" -> duration;
        case "days" -> duration * 24L * 60L;
        default -> duration * 60L;
      };
      ZonedDateTime end = ZonedDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(minutes);
      it.closesAt = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    repo.create(it);
    return ResponseEntity.ok(it);
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<?> status(@PathVariable int id, @RequestParam String status){
    return repo.updateStatus(id, status) == 0 ? ResponseEntity.notFound().build() : ResponseEntity.ok().build();
  }

  @PatchMapping("/{id}/price")
  public ResponseEntity<?> price(@PathVariable int id, @RequestParam("newPrice") double newPrice){
    if (newPrice < 0) return ResponseEntity.badRequest().body(Map.of("error","price must be >=0"));
    return repo.updateDutchPrice(id, newPrice) == 0 ? ResponseEntity.badRequest().body(Map.of("error","not updated")) : ResponseEntity.ok().build();
  }
}

