package ca.yorku.itec4020.payment.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/payments")
public class PaymentsController {

  private final WebClient catalogueClient;
  private final WebClient auctionClient;

  public PaymentsController(
      @Value("${services.catalogue.base-url}") String catalogueBaseUrl,
      @Value("${services.auction.base-url}") String auctionBaseUrl
  ) {
    this.catalogueClient = WebClient.builder().baseUrl(catalogueBaseUrl).build();
    this.auctionClient = WebClient.builder().baseUrl(auctionBaseUrl).build();
  }

  public record PaymentRequest(
      @Min(1) long itemId,
      @Min(1) long userId,
      @Positive int amount,
      boolean expeditedShipping,
      @NotBlank String addressStreet,
      @NotBlank String addressCity,
      @NotBlank String addressCountry,
      @NotBlank String postalCode,
      @NotBlank String cardNumber,
      @NotBlank String cardName,
      @NotBlank String cardExpiry,
      @NotBlank String cardCvv
  ) {}

  public record Receipt(
      String status,
      long itemId,
      long userId,
      int amountPaid,
      int shippingCost,
      int itemPrice,
      int totalCharged,
      int shippingTimeDays,
      Instant paidAt,
      String message
  ){}

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> pay(@Valid @RequestBody PaymentRequest req) {
    // Lookup item shipping info
    Map item = catalogueClient.get().uri("/items/" + req.itemId())
        .retrieve().bodyToMono(Map.class).block();
    if (item == null) {
      return ResponseEntity.status(404).body(Map.of("error", "Item not found"));
    }
    int shipRegular = ((Number) item.getOrDefault("shippingCostRegular", 0)).intValue();
    int shipExpedited = ((Number) item.getOrDefault("shippingCostExpedited", 0)).intValue();
    int shippingDays = ((Number) item.getOrDefault("shippingTimeDays", 0)).intValue();
    int shippingCost = req.expeditedShipping() ? shipExpedited : shipRegular;

    // Lookup auction to get final price
    Map auction = auctionClient.get().uri("/auctions/" + req.itemId())
        .retrieve().bodyToMono(Map.class).block();
    if (auction == null || auction.get("currentPrice") == null) {
      return ResponseEntity.status(400).body(Map.of("error", "Auction state unavailable"));
    }
    // Allow payment only when auction has ended and requester is the winner
    Object status = auction.get("status");
    if (status == null || !"CLOSED".equals(String.valueOf(status))) {
      return ResponseEntity.status(400).body(Map.of("error", "Auction has not ended"));
    }
    Object winnerId = auction.get("highestBidderId");
    if (winnerId == null || !String.valueOf(winnerId).equals(String.valueOf(req.userId()))) {
      return ResponseEntity.status(403).body(Map.of("error", "Only the winning bidder can pay for this item"));
    }
  int itemPrice = ((Number) auction.get("currentPrice")).intValue();
    int expectedTotal = itemPrice + shippingCost;
    if (req.amount() != expectedTotal) {
      return ResponseEntity.status(400).body(Map.of("error", "Amount must equal item price + shipping"));
    }

    // Mark item as sold in catalogue (best-effort)
    try {
      catalogueClient.post().uri("/items/" + req.itemId() + "/sold")
          .retrieve().bodyToMono(Void.class).block();
    } catch (Exception ignore) { /* log in real system */ }

    // In a real system, validate card and capture payment here.
    return ResponseEntity.ok(new Receipt(
        "PAID",
        req.itemId(),
        req.userId(),
        req.amount(),
        shippingCost,
        itemPrice,
        expectedTotal,
        shippingDays,
        Instant.now(),
        "Payment accepted (mock)"
    ));
  }
}
