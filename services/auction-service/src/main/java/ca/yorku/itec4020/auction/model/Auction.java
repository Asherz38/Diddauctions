package ca.yorku.itec4020.auction.model;

import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;

import java.time.Instant;

public class Auction {
  public enum Status { OPEN, CLOSED }

  private long itemId;
  private AuctionType type;
  private int currentPrice;
  private Long highestBidderId;
  private Instant endTime;
  private Status status = Status.OPEN;

  public long getItemId() { return itemId; }
  public void setItemId(long itemId) { this.itemId = itemId; }
  public AuctionType getType() { return type; }
  public void setType(AuctionType type) { this.type = type; }
  public int getCurrentPrice() { return currentPrice; }
  public void setCurrentPrice(int currentPrice) { this.currentPrice = currentPrice; }
  public Long getHighestBidderId() { return highestBidderId; }
  public void setHighestBidderId(Long highestBidderId) { this.highestBidderId = highestBidderId; }
  public Instant getEndTime() { return endTime; }
  public void setEndTime(Instant endTime) { this.endTime = endTime; }
  public Status getStatus() { return status; }
  public void setStatus(Status status) { this.status = status; }
}
