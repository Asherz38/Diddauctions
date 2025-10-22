package ca.yorku.itec4020.catalogue.model;

import ca.yorku.itec4020.common.dto.ItemDtos.AuctionType;
import ca.yorku.itec4020.common.dto.ItemDtos.Condition;

public class Item {
  private long id;
  private String name;
  private String description;
  private String category;
  private String imageUrl;
  private AuctionType auctionType;
  private String keywords;
  private int startingPrice;
  private Integer reservePrice;
  private int shippingCostRegular;
  private int shippingCostExpedited;
  private int shippingTimeDays;
  private Condition condition;
  private int quantity;
  private boolean available = true;

  // getters and setters
  public long getId() { return id; }
  public void setId(long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public AuctionType getAuctionType() { return auctionType; }
  public void setAuctionType(AuctionType auctionType) { this.auctionType = auctionType; }
  public String getKeywords() { return keywords; }
  public void setKeywords(String keywords) { this.keywords = keywords; }
  public int getStartingPrice() { return startingPrice; }
  public void setStartingPrice(int startingPrice) { this.startingPrice = startingPrice; }
  public Integer getReservePrice() { return reservePrice; }
  public void setReservePrice(Integer reservePrice) { this.reservePrice = reservePrice; }
  public int getShippingCostRegular() { return shippingCostRegular; }
  public void setShippingCostRegular(int shippingCostRegular) { this.shippingCostRegular = shippingCostRegular; }
  public int getShippingCostExpedited() { return shippingCostExpedited; }
  public void setShippingCostExpedited(int shippingCostExpedited) { this.shippingCostExpedited = shippingCostExpedited; }
  public int getShippingTimeDays() { return shippingTimeDays; }
  public void setShippingTimeDays(int shippingTimeDays) { this.shippingTimeDays = shippingTimeDays; }
  public Condition getCondition() { return condition; }
  public void setCondition(Condition condition) { this.condition = condition; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int quantity) { this.quantity = quantity; }
  public boolean isAvailable() { return available; }
  public void setAvailable(boolean available) { this.available = available; }
}
