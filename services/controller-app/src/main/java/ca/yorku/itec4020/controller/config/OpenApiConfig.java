package ca.yorku.itec4020.controller.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI api() {
    return new OpenAPI().info(new Info()
        .title("Auction Gateway API")
        .description("Interactive docs for the controller-app gateway under /api")
        .version("v1"));
  }
}
