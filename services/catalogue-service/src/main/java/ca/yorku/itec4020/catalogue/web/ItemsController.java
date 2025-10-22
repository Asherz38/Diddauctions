package ca.yorku.itec4020.catalogue.web;

import ca.yorku.itec4020.catalogue.service.ItemService;
import ca.yorku.itec4020.common.dto.ApiError;
import ca.yorku.itec4020.common.dto.ItemDtos.CreateItemRequest;
import ca.yorku.itec4020.common.dto.ItemDtos.ItemResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/items")
public class ItemsController {
  private final ItemService itemService;

  public ItemsController(ItemService itemService) {
    this.itemService = itemService;
  }

  @PostMapping
  public ResponseEntity<?> create(@Valid @RequestBody CreateItemRequest req) {
    return ResponseEntity.ok(itemService.create(req));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> get(@PathVariable("id") long id) {
    return itemService.get(id)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Item not found")));
  }

  @GetMapping("/search")
  public List<ItemResponse> search(@RequestParam(name = "q", required = false) String q) {
    return itemService.search(q);
  }

  @PostMapping("/{id}/sold")
  public ResponseEntity<?> markSold(@PathVariable("id") long id) {
    var existing = itemService.get(id);
    if (existing.isEmpty()) {
      return ResponseEntity.status(404).body(new ApiError("Item not found"));
    }
    var updated = itemService.markSold(id);
    return ResponseEntity.ok(updated);
  }
}
