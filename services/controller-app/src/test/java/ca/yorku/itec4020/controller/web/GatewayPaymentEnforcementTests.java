package ca.yorku.itec4020.controller.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayPaymentEnforcementTests {
  private static MockWebServer userServer;
  private static MockWebServer auctionServer;
  private static MockWebServer paymentServer;

  @LocalServerPort
  int port;

  @Autowired
  ObjectMapper objectMapper;

  static String userBaseUrl;
  static String auctionBaseUrl;
  static String paymentBaseUrl;

  @BeforeAll
  static void setup() throws IOException {
    userServer = new MockWebServer();
    auctionServer = new MockWebServer();
    paymentServer = new MockWebServer();
    userServer.start();
    auctionServer.start();
    paymentServer.start();
    userBaseUrl = "http://" + userServer.getHostName() + ":" + userServer.getPort();
    auctionBaseUrl = "http://" + auctionServer.getHostName() + ":" + auctionServer.getPort();
    paymentBaseUrl = "http://" + paymentServer.getHostName() + ":" + paymentServer.getPort();
  }

  @AfterAll
  static void tearDown() throws IOException {
    userServer.shutdown();
    auctionServer.shutdown();
    paymentServer.shutdown();
  }

  @DynamicPropertySource
  static void registerProps(DynamicPropertyRegistry registry) {
    registry.add("services.user.base-url", () -> userBaseUrl);
    registry.add("services.auction.base-url", () -> auctionBaseUrl);
    registry.add("services.payment.base-url", () -> paymentBaseUrl);
  }

  @Test
  void blocksNonWinnerPayment() throws Exception {
    // user says caller is userId=2
  userServer.enqueue(new MockResponse().setResponseCode(200)
    .addHeader("Content-Type", "application/json")
    .setBody("{\"userId\":2}"));
    // auction CLOSED with winner userId=1 at price 120
  auctionServer.enqueue(new MockResponse().setResponseCode(200)
    .addHeader("Content-Type", "application/json")
    .setBody("{\"status\":\"CLOSED\",\"highestBidderId\":1,\"currentPrice\":120}"));

    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer tokenX");
    String body = "{\"itemId\":123,\"userId\":2,\"amount\":120}";
    try {
      rt.postForEntity("http://localhost:" + port + "/api/payments",
          new HttpEntity<>(body, headers), String.class);
      fail("Expected 403 Forbidden");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      assertEquals(403, e.getStatusCode().value());
    }
    // ensure payment service not called
    assertNull(paymentServer.takeRequest(100, java.util.concurrent.TimeUnit.MILLISECONDS));
  }

  @Test
  void forwardsWhenWinnerAndAmountMatches() throws Exception {
    // user is 1
  userServer.enqueue(new MockResponse().setResponseCode(200)
    .addHeader("Content-Type", "application/json")
    .setBody("{\"userId\":1}"));
    // auction CLOSED winner 1 price 120
  auctionServer.enqueue(new MockResponse().setResponseCode(200)
    .addHeader("Content-Type", "application/json")
    .setBody("{\"status\":\"CLOSED\",\"highestBidderId\":1,\"currentPrice\":120}"));
    // payment server returns 200 OK with a simple receipt
  paymentServer.enqueue(new MockResponse().setResponseCode(200)
    .addHeader("Content-Type", "application/json")
    .setBody("{\"status\":\"PAID\"}"));

    RestTemplate rt = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer tokenY");
    String body = "{\"itemId\":123,\"userId\":999,\"amount\":120}"; // userId will be overridden
    ResponseEntity<String> resp = rt.postForEntity("http://localhost:" + port + "/api/payments",
        new HttpEntity<>(body, headers), String.class);

    assertEquals(200, resp.getStatusCode().value());
    // verify a request reached payment service
    assertNotNull(paymentServer.takeRequest());
  }
}
