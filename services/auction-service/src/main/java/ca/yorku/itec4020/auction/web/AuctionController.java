package ca.yorku.itec4020.auction.web;

import ca.yorku.itec4020.auction.model.Auction;
import ca.yorku.itec4020.auction.service.AuctionService;
import ca.yorku.itec4020.common.dto.ApiError;
import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/auctions")
public class AuctionController {
  private final AuctionService auctionService;

  public AuctionController(AuctionService auctionService) {
    this.auctionService = auctionService;
  }

  public record StartRequest(long itemId, AuctionType type, @Min(0) int startingPrice, @Min(1) long durationSeconds, Integer reservePrice) {}

  @PostMapping("/start")
  public Auction start(@Valid @RequestBody StartRequest req) {
    return auctionService.start(req.itemId(), req.type(), req.startingPrice(), Duration.ofSeconds(req.durationSeconds()), req.reservePrice());
  }

  public record BidRequest(long bidderId, @Min(1) int amount) {}

  @PostMapping("/{itemId}/bids")
  public ResponseEntity<?> bid(@PathVariable("itemId") long itemId, @Valid @RequestBody BidRequest req) {
    return auctionService.get(itemId)
        .<ResponseEntity<?>>map(a -> {
          if (a.getStatus() == Auction.Status.CLOSED) {
            return ResponseEntity.badRequest().body(new ApiError("Auction is closed"));
          }
          if (a.getType() != AuctionType.FORWARD) {
            return ResponseEntity.badRequest().body(new ApiError("Bids are only allowed for FORWARD auctions"));
          }
          if (req.amount() <= a.getCurrentPrice()) {
            return ResponseEntity.badRequest().body(new ApiError("Bid must be greater than current price"));
          }
          return auctionService.bid(itemId, req.bidderId(), req.amount())
              .<ResponseEntity<?>>map(ResponseEntity::ok)
              .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
        })
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
  }

  public record BuyNowRequest(long bidderId) {}

  @PostMapping("/{itemId}/buy-now")
  public ResponseEntity<?> buyNow(@PathVariable("itemId") long itemId, @Valid @RequestBody BuyNowRequest req) {
    return auctionService.get(itemId)
        .<ResponseEntity<?>>map(a -> {
          if (a.getStatus() == Auction.Status.CLOSED) {
            return ResponseEntity.badRequest().body(new ApiError("Auction is closed"));
          }
          if (a.getType() != AuctionType.DUTCH) {
            return ResponseEntity.badRequest().body(new ApiError("Buy-now is only available for DUTCH auctions"));
          }
          return auctionService.buyNow(itemId, req.bidderId())
              .<ResponseEntity<?>>map(ResponseEntity::ok)
              .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
        })
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
  }

  public record DecreaseRequest(@Min(1) int decrement) {}

  @PostMapping("/{itemId}/decrease")
  public ResponseEntity<?> decrease(@PathVariable("itemId") long itemId, @Valid @RequestBody DecreaseRequest req) {
    return auctionService.decreaseDutchPrice(itemId, req.decrement())
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
  }

  public record WithdrawRequest(long bidderId) {}

  @PostMapping("/{itemId}/withdraw")
  public ResponseEntity<?> withdraw(@PathVariable("itemId") long itemId, @Valid @RequestBody WithdrawRequest req) {
    return auctionService.withdrawLastBid(itemId, req.bidderId())
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
  }

  @GetMapping("/{itemId}")
  public ResponseEntity<?> get(@PathVariable("itemId") long itemId) {
    return auctionService.get(itemId)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).body(new ApiError("Auction not found")));
  }
}
