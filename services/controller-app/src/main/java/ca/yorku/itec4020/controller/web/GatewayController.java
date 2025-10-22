package ca.yorku.itec4020.controller.web;

import ca.yorku.itec4020.common.dto.ItemDtos.CreateItemRequest;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {
  private final WebClient catalogueClient;
  private final WebClient auctionClient;
  private final WebClient paymentClient;
  private final WebClient userClient;
  private final ObjectMapper objectMapper;

  public GatewayController(
      @Value("${services.catalogue.base-url}") String catalogueBaseUrl,
      @Value("${services.auction.base-url}") String auctionBaseUrl,
      @Value("${services.payment.base-url}") String paymentBaseUrl,
      @Value("${services.user.base-url}") String userBaseUrl,
      ObjectMapper objectMapper
  ) {
    this.catalogueClient = WebClient.builder().baseUrl(catalogueBaseUrl).build();
    this.auctionClient = WebClient.builder().baseUrl(auctionBaseUrl).build();
    this.paymentClient = WebClient.builder().baseUrl(paymentBaseUrl).build();
    this.userClient = WebClient.builder().baseUrl(userBaseUrl).build();
    this.objectMapper = objectMapper;
  }

  @GetMapping("/items/search")
  public Mono<ResponseEntity<String>> search(@RequestParam(name = "q", required = false) String q) {
    return catalogueClient.get()
        .uri(uri -> uri.path("/items/search").queryParam("q", q).build())
        .retrieve().toEntity(String.class);
  }

  @PostMapping(value = "/items", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> createItem(@Valid @RequestBody CreateItemRequest body) {
    String payload = catalogueClient.post().uri("/items")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(String.class)
        .block();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload);
  }

  @PostMapping(value = "/auctions/start", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> startAuction(@RequestBody String body) {
    String payload = auctionClient.post().uri("/auctions/start")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(String.class)
        .block();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload);
  }

  @PostMapping(value = "/auctions/{itemId}/bids", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> bid(@PathVariable long itemId, @RequestBody String body) {
    String payload = auctionClient.post().uri("/auctions/" + itemId + "/bids")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(String.class)
        .block();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload);
  }

  @PostMapping(value = "/auctions/{itemId}/buy-now", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> buyNow(@PathVariable long itemId, @RequestBody String body) {
    String payload = auctionClient.post().uri("/auctions/" + itemId + "/buy-now")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(String.class)
        .block();
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload);
  }

  // Minimal view of auction status for validation
  public static record AuctionView(String status, Long highestBidderId, Integer currentPrice) {}

  @PostMapping(value = "/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<String>> pay(
      @RequestHeader(name = "Authorization", required = false) String auth,
      @RequestBody String body
  ) {
    // Parse incoming payment to read itemId, userId, amount
    final JsonNode root;
    try {
      root = objectMapper.readTree(body);
    } catch (Exception e) {
      return Mono.just(ResponseEntity.badRequest().body("{\"error\":\"Invalid JSON\"}"));
    }

    if (auth == null || !auth.startsWith("Bearer ")) {
      return Mono.just(ResponseEntity.status(401).body("{\"error\":\"Missing token\"}"));
    }

    final long itemId = root.path("itemId").asLong(0);
    final long bodyUserId = root.path("userId").asLong(0);
    final int amount = root.path("amount").asInt(0);
    if (itemId <= 0 || amount <= 0) {
      return Mono.just(ResponseEntity.badRequest().body("{\"error\":\"Missing itemId/amount\"}"));
    }

    // 1) Identify caller from token
    Mono<Map> meMono = userClient.get().uri("/users/me")
        .header("Authorization", auth)
        .retrieve().bodyToMono(Map.class);

    // 2) Load auction status
    Mono<AuctionView> auctionMono = auctionClient.get().uri("/auctions/" + itemId)
        .retrieve().bodyToMono(AuctionView.class);

    return Mono.zip(meMono, auctionMono)
        .flatMap(tuple -> {
          Map me = tuple.getT1();
          AuctionView auction = tuple.getT2();
          Object uidObj = me.get("userId");
          if (uidObj == null) {
            return Mono.just(ResponseEntity.status(401).body("{\"error\":\"Invalid token\"}"));
          }
          long tokenUserId = ((Number) uidObj).longValue();

          // Enforce userId in body matches token; if not, override to prevent spoofing
          ObjectNode patched = root.deepCopy();
          patched.put("userId", tokenUserId);

          if (auction == null) {
            return Mono.just(ResponseEntity.status(404).body("{\"error\":\"Auction not found\"}"));
          }
          if (!"CLOSED".equalsIgnoreCase(auction.status())) {
            return Mono.just(ResponseEntity.status(400).body("{\"error\":\"Auction not closed\"}"));
          }
          if (auction.highestBidderId() == null || auction.highestBidderId() != tokenUserId) {
            return Mono.just(ResponseEntity.status(403).body("{\"error\":\"Only winner can pay\"}"));
          }
          // Amount validation is delegated to payment-service (it verifies item price + shipping).

          String forwardBody;
          try {
            forwardBody = objectMapper.writeValueAsString(patched);
          } catch (Exception e) {
            return Mono.just(ResponseEntity.internalServerError().body("{\"error\":\"Failed to serialize payment\"}"));
          }

          // 3) Forward to payment service on success
          return paymentClient.post().uri("/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(forwardBody)
        .retrieve().bodyToMono(String.class)
        .map(json -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json));
        })
        .onErrorResume(ex -> Mono.just(ResponseEntity.status(502).body("{\"error\":\"Upstream error\"}")));
  }
}
