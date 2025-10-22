package ca.yorku.itec4020.common.dto;

import jakarta.validation.constraints.*;

public class ItemDtos {
  public enum AuctionType { FORWARD, DUTCH }
  public enum Condition { NEW, USED, REFURBISHED }

  public record CreateItemRequest(
      @NotBlank String name,
      @NotBlank String description,
    @NotBlank String category,
    String imageUrl,
      @NotNull AuctionType auctionType,
      @NotBlank String keywords,
      @NotNull @PositiveOrZero Integer startingPrice,
      @PositiveOrZero Integer reservePrice,
      @NotNull @PositiveOrZero Integer shippingCostRegular,
      @NotNull @PositiveOrZero Integer shippingCostExpedited,
      @NotNull @Positive Integer shippingTimeDays,
      @NotNull Condition condition,
      @NotNull @Positive Integer quantity
  ) {}

  public record ItemResponse(
      long id,
      String name,
      String description,
    String category,
    String imageUrl,
      AuctionType auctionType,
      String keywords,
      int startingPrice,
      Integer reservePrice,
      int shippingCostRegular,
      int shippingCostExpedited,
      int shippingTimeDays,
      Condition condition,
      int quantity,
      boolean available
  ) {}
}
