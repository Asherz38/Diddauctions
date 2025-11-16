package com.example.catalogue;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogueServer {
  private final ItemRepository repo;
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public CatalogueServer(ItemRepository repo) { this.repo = repo; }

  public List<Item> listAvailable(String q) {
    var items = repo.listActive(q);
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return items.stream().filter(it -> {
      if ("FORWARD".equalsIgnoreCase(it.auctionType) && it.closesAt != null && !it.closesAt.isBlank()) {
        try {
          LocalDateTime end = LocalDateTime.parse(it.closesAt, FMT);
          return end.isAfter(now);
        } catch (Exception ignored) { return true; }
      }
      return true; 
    }).collect(Collectors.toList());
  }

  public Item find(int id) { return repo.find(id).orElse(null); }

  public Item create(Item it, Integer duration, String unit) {
    if (it.auctionType == null) it.auctionType = "FORWARD";
    if (it.startingPrice == null) it.startingPrice = 0d;
    if (it.currentPrice == null) it.currentPrice = it.startingPrice;
    it.status = "ACTIVE";
    if ("FORWARD".equalsIgnoreCase(it.auctionType)) {
      if (duration == null || duration <= 0) {
        throw new IllegalArgumentException("duration required for forward auctions");
      }
      long minutes = switch (unit == null ? "hours" : unit.toLowerCase()) {
        case "minutes" -> duration;
        case "days" -> duration * 24L * 60L;
        default -> duration * 60L;
      };
      LocalDateTime end = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(minutes);
      it.closesAt = end.format(FMT);
    }
    repo.create(it);
    return it;
  }

  public int updateStatus(int id, String status) {
     return repo.updateStatus(id, status); 
  }
  public int updateDutchPrice(int id, double newPrice) { 
    return repo.updateDutchPrice(id, newPrice); 
  }
  public int delete(int id) { 
    return repo.delete(id); 
  }
}


