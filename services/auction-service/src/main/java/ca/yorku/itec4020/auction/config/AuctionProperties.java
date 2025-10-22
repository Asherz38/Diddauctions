package ca.yorku.itec4020.auction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auction")
public class AuctionProperties {
  /**
   * Time window in seconds during which the last bidder can withdraw their bid.
   */
  private long withdrawWindowSeconds = 30;

  public long getWithdrawWindowSeconds() {
    return withdrawWindowSeconds;
  }

  public void setWithdrawWindowSeconds(long withdrawWindowSeconds) {
    this.withdrawWindowSeconds = withdrawWindowSeconds;
  }
}
