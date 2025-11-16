package com.example.auth.model;

public class TopBid {
  private final Double amount;
  private final String username;

  public TopBid(Double amount, String username) {
    this.amount = amount;
    this.username = username;
  }

  public Double getAmount() {
    return amount;
  }

  public String getUsername() {
    return username;
  }
}

