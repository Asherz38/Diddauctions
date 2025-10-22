package ca.yorku.itec4020.catalogue.service;

import ca.yorku.itec4020.catalogue.entity.ItemEntity;
import ca.yorku.itec4020.catalogue.repo.ItemRepository;
import ca.yorku.itec4020.common.dto.ItemDtos.CreateItemRequest;
import ca.yorku.itec4020.common.dto.ItemDtos.ItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
  private final ItemRepository repo;

  public ItemService(ItemRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public ItemResponse create(CreateItemRequest req) {
    ItemEntity e = new ItemEntity();
    e.setName(req.name());
    e.setDescription(req.description());
    e.setCategory(req.category());
    e.setImageUrl(req.imageUrl());
    e.setAuctionType(req.auctionType());
    e.setKeywords(req.keywords());
    e.setStartingPrice(req.startingPrice());
    e.setReservePrice(req.reservePrice());
    e.setShippingCostRegular(req.shippingCostRegular());
    e.setShippingCostExpedited(req.shippingCostExpedited());
    e.setShippingTimeDays(req.shippingTimeDays());
    e.setCondition(req.condition());
    e.setQuantity(req.quantity());
    e.setAvailable(true);
    e = repo.save(e);
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public Optional<ItemResponse> get(long id) {
    return repo.findById(id).map(this::toResponse);
  }

  @Transactional
  public ItemResponse markSold(long id) {
    ItemEntity e = repo.findById(id).orElseThrow();
    int qty = e.getQuantity();
    if (qty > 0) {
      e.setQuantity(qty - 1);
    }
    if (e.getQuantity() <= 0) {
      e.setAvailable(false);
    }
    e = repo.save(e);
    return toResponse(e);
  }

  @Transactional(readOnly = true)
  public List<ItemResponse> search(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }
    return repo.search(keyword).stream().map(this::toResponse).collect(Collectors.toList());
  }

  private ItemResponse toResponse(ItemEntity i) {
    return new ItemResponse(
        i.getId(),
        i.getName(),
        i.getDescription(),
        i.getCategory(),
        i.getImageUrl(),
        i.getAuctionType(),
        i.getKeywords(),
        i.getStartingPrice(),
        i.getReservePrice(),
        i.getShippingCostRegular(),
        i.getShippingCostExpedited(),
        i.getShippingTimeDays(),
        i.getCondition(),
        i.getQuantity(),
        i.isAvailable()
    );
  }
}
