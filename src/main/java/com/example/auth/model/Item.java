package com.example.auth.model;

public class Item {
  private Integer id;
  private String name;
  private String description;
  private String status; 
  private Double startingPrice;
  private Double currentPrice;
  private String auctionType; 
  private String closesAt; 
  private Long remainingSeconds; 
  private Double shippingPrice;
  private Double expeditePrice;
  private Integer shippingDays;
  private Integer sellerUserId;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Double getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(Double startingPrice) {
    this.startingPrice = startingPrice;
  }

  public Double getCurrentPrice() {
    return currentPrice;
  }

  public void setCurrentPrice(Double currentPrice) {
    this.currentPrice = currentPrice;
  }

  public String getAuctionType() {
    return auctionType;
  }

  public void setAuctionType(String auctionType) {
    this.auctionType = auctionType;
  }

  public String getClosesAt() {
    return closesAt;
  }

  public void setClosesAt(String closesAt) {
    this.closesAt = closesAt;
  }

  public Long getRemainingSeconds() {
    return remainingSeconds;
  }

  public void setRemainingSeconds(Long remainingSeconds) {
    this.remainingSeconds = remainingSeconds;
  }

  public Double getShippingPrice() {
    return shippingPrice;
  }

  public void setShippingPrice(Double shippingPrice) {
    this.shippingPrice = shippingPrice;
  }

  public Double getExpeditePrice() {
    return expeditePrice;
  }

  public void setExpeditePrice(Double expeditePrice) {
    this.expeditePrice = expeditePrice;
  }

  public Integer getShippingDays() {
    return shippingDays;
  }

  public void setShippingDays(Integer shippingDays) {
    this.shippingDays = shippingDays;
  }

  public Integer getSellerUserId() {
    return sellerUserId;
  }

  public void setSellerUserId(Integer sellerUserId) {
    this.sellerUserId = sellerUserId;
  }
}

