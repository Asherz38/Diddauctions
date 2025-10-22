package ca.yorku.itec4020.auction;

import ca.yorku.itec4020.auction.config.AuctionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {"ca.yorku.itec4020"})
@EnableConfigurationProperties(AuctionProperties.class)
public class AuctionServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AuctionServiceApplication.class, args);
  }
}
