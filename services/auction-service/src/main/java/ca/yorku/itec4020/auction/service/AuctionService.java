package ca.yorku.itec4020.auction.service;

import ca.yorku.itec4020.auction.config.AuctionProperties;
import ca.yorku.itec4020.auction.entity.AuctionEntity;
import ca.yorku.itec4020.auction.entity.BidEntity;
import ca.yorku.itec4020.auction.model.Auction;
import ca.yorku.itec4020.auction.repo.AuctionRepository;
import ca.yorku.itec4020.auction.repo.BidRepository;
import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class AuctionService {
  private final AuctionProperties props;

  private final AuctionRepository repo;
  private final BidRepository bids;

  public AuctionService(AuctionRepository repo, BidRepository bids, AuctionProperties props) {
    this.repo = repo;
    this.bids = bids;
    this.props = props;
  }

  @Transactional
  public Auction start(long itemId, AuctionType type, int startingPrice, Duration duration, Integer reservePrice) {
    AuctionEntity e = new AuctionEntity();
    e.setItemId(itemId);
    e.setType(type);
    e.setStartingPrice(startingPrice);
    e.setReservePrice(reservePrice);
    e.setCurrentPrice(startingPrice);
    e.setEndTime(Instant.now().plus(duration));
    e.setStatus(AuctionEntity.Status.OPEN);
    e = repo.save(e);
    return toModel(e);
  }

  @Transactional(readOnly = true)
  public Optional<Auction> get(long itemId) {
    return repo.findById(itemId).map(e -> {
      if (e.getStatus() == AuctionEntity.Status.OPEN && Instant.now().isAfter(e.getEndTime())) {
        Auction m = toModel(e);
        m.setStatus(Auction.Status.CLOSED);
        return m;
      }
      return toModel(e);
    });
  }

  @Transactional
  public Optional<Auction> bid(long itemId, long bidderId, int amount) {
    return repo.findById(itemId).map(e -> {
      if (e.getStatus() == AuctionEntity.Status.CLOSED || Instant.now().isAfter(e.getEndTime())) {
        e.setStatus(AuctionEntity.Status.CLOSED);
        return toModel(e);
      }
      if (e.getType() == AuctionType.FORWARD && amount > e.getCurrentPrice()) {
        e.setCurrentPrice(amount);
        e.setHighestBidderId(bidderId);
        repo.save(e);
        BidEntity b = new BidEntity();
        b.setItemId(itemId);
        b.setBidderId(bidderId);
        b.setAmount(amount);
        bids.save(b);
      }
      return toModel(e);
    });
  }

  @Transactional
  public Optional<Auction> buyNow(long itemId, long bidderId) {
    return repo.findById(itemId).map(e -> {
      if (e.getStatus() == AuctionEntity.Status.CLOSED || Instant.now().isAfter(e.getEndTime())) {
        e.setStatus(AuctionEntity.Status.CLOSED);
        return toModel(e);
      }
      if (e.getType() == AuctionType.DUTCH) {
        e.setHighestBidderId(bidderId);
        e.setStatus(AuctionEntity.Status.CLOSED);
        repo.save(e);
      }
      return toModel(e);
    });
  }

  @Transactional
  public Optional<Auction> decreaseDutchPrice(long itemId, int decrement) {
    if (decrement <= 0) return get(itemId);
    return repo.findById(itemId).map(e -> {
      if (e.getType() != AuctionType.DUTCH || e.getStatus() == AuctionEntity.Status.CLOSED) return toModel(e);
      int newPrice = e.getCurrentPrice() - decrement;
      Integer reserve = e.getReservePrice();
      if (reserve != null && newPrice < reserve) newPrice = reserve;
      if (newPrice < 0) newPrice = 0;
      e.setCurrentPrice(newPrice);
      repo.save(e);
      return toModel(e);
    });
  }

  @Transactional
  public Optional<Auction> withdrawLastBid(long itemId, long bidderId) {
    return repo.findById(itemId).map(e -> {
      if (e.getType() != AuctionType.FORWARD || e.getStatus() == AuctionEntity.Status.CLOSED) return toModel(e);
      return bids.findLast(itemId).map(last -> {
        Instant now = Instant.now();
        if (!last.getBidderId().equals(bidderId) || now.isAfter(last.getCreatedAt().plusSeconds(props.getWithdrawWindowSeconds()))) {
          return toModel(e);
        }
        bids.deleteById(last.getId());
        // recompute current price from previous bid
        int current = e.getStartingPrice();
        var history = bids.listByItemId(itemId);
        Long highestBidder = null;
        for (BidEntity b : history) {
          if (b.getAmount() > current) {
            current = b.getAmount();
            highestBidder = b.getBidderId();
          }
        }
        e.setCurrentPrice(current);
        e.setHighestBidderId(highestBidder);
        repo.save(e);
        return toModel(e);
      }).orElseGet(() -> toModel(e));
    });
  }

  private Auction toModel(AuctionEntity e) {
    Auction m = new Auction();
    m.setItemId(e.getItemId());
    m.setType(e.getType());
    m.setCurrentPrice(e.getCurrentPrice());
    m.setHighestBidderId(e.getHighestBidderId());
    m.setEndTime(e.getEndTime());
    m.setStatus(e.getStatus() == AuctionEntity.Status.OPEN ? Auction.Status.OPEN : Auction.Status.CLOSED);
    return m;
  }
}
