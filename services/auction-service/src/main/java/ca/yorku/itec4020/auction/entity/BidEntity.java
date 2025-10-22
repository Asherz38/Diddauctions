package ca.yorku.itec4020.auction.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bids")
public class BidEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long itemId;
  private Long bidderId;
  private int amount;
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getItemId() { return itemId; }
  public void setItemId(Long itemId) { this.itemId = itemId; }
  public Long getBidderId() { return bidderId; }
  public void setBidderId(Long bidderId) { this.bidderId = bidderId; }
  public int getAmount() { return amount; }
  public void setAmount(int amount) { this.amount = amount; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
