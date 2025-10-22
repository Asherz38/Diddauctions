package ca.yorku.itec4020.auction.service;

import ca.yorku.itec4020.auction.model.Auction;
import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuctionServiceTests {

  @Autowired
  private AuctionService auctionService;

  @Test
  void withdrawLastBid_withinWindow_resetsPriceAndWinner() {
    long itemId = 1001L;
    auctionService.start(itemId, AuctionType.FORWARD, 100, Duration.ofSeconds(60), null);

    // place a bid
    auctionService.bid(itemId, 1L, 120).orElseThrow();

    // withdraw immediately (within 30s window)
    Auction after = auctionService.withdrawLastBid(itemId, 1L).orElseThrow();

    assertEquals(100, after.getCurrentPrice(), "current price should reset to starting price after withdrawal");
    assertNull(after.getHighestBidderId(), "highest bidder should be cleared after withdrawal");
    assertEquals(Auction.Status.OPEN, after.getStatus());
  }
}
