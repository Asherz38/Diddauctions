package ca.yorku.itec4020.auction.service;

import ca.yorku.itec4020.auction.model.Auction;
import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuctionServiceDutchTests {

  @Autowired
  private AuctionService auctionService;

  @Test
  void dutchDecrease_respectsReserve() {
    long itemId = 2001L;
    // startingPrice 500, reserve 300
    auctionService.start(itemId, AuctionType.DUTCH, 500, Duration.ofSeconds(60), 300);

    // decrease by 50 -> 450
    Auction a1 = auctionService.decreaseDutchPrice(itemId, 50).orElseThrow();
    assertEquals(450, a1.getCurrentPrice());

    // big decrease by 1000 -> should clamp to reserve 300
    Auction a2 = auctionService.decreaseDutchPrice(itemId, 1000).orElseThrow();
    assertEquals(300, a2.getCurrentPrice());
  }

  @Test
  void dutchDecrease_notBelowZero_whenNoReserve() {
    long itemId = 2002L;
    auctionService.start(itemId, AuctionType.DUTCH, 10, Duration.ofSeconds(60), null);

    Auction a1 = auctionService.decreaseDutchPrice(itemId, 15).orElseThrow();
    assertEquals(0, a1.getCurrentPrice());
  }
}
