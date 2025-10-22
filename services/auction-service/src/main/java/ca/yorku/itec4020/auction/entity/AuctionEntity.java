package ca.yorku.itec4020.auction.entity;

import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auctions")
public class AuctionEntity {
  @Id
  private Long itemId; // one auction per itemId

  @Enumerated(EnumType.STRING)
  private AuctionType type;

  private int startingPrice;
  private Integer reservePrice;
  private int currentPrice;
  private Long highestBidderId;
  private Instant endTime;

  @Enumerated(EnumType.STRING)
  private Status status = Status.OPEN;

  @Version
  private Long version;

  public enum Status { OPEN, CLOSED }

  public Long getItemId() { return itemId; }
  public void setItemId(Long itemId) { this.itemId = itemId; }
  public AuctionType getType() { return type; }
  public void setType(AuctionType type) { this.type = type; }
  public int getStartingPrice() { return startingPrice; }
  public void setStartingPrice(int startingPrice) { this.startingPrice = startingPrice; }
  public Integer getReservePrice() { return reservePrice; }
  public void setReservePrice(Integer reservePrice) { this.reservePrice = reservePrice; }
  public int getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(int currentPrice) { this.currentPrice = currentPrice; }
  public Long getHighestBidderId() { return highestBidderId; }
  public void setHighestBidderId(Long highestBidderId) { this.highestBidderId = highestBidderId; }
  public Instant getEndTime() { return endTime; }
  public void setEndTime(Instant endTime) { this.endTime = endTime; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
