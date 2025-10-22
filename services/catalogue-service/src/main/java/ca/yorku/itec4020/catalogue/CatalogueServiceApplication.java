package ca.yorku.itec4020.catalogue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ca.yorku.itec4020"})
public class CatalogueServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(CatalogueServiceApplication.class, args);
  }
}
