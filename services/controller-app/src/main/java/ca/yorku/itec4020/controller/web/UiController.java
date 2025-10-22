package ca.yorku.itec4020.controller.web;

import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@Validated
@RequestMapping("/ui")
public class UiController {
  private final WebClient catalogue;
  private final WebClient auction;
  private final WebClient payment;

  private static final String SESSION_TOKEN = "ui.token";
  private static final String SESSION_USER_ID = "ui.userId";
  private static final String SESSION_LOCKED_ITEM = "ui.lockedItemId";

  public UiController(
      @Value("${services.catalogue.base-url}") String catalogueBaseUrl,
      @Value("${services.auction.base-url}") String auctionBaseUrl,
      @Value("${services.payment.base-url}") String paymentBaseUrl
  ) {
    this.catalogue = WebClient.builder().baseUrl(catalogueBaseUrl).build();
    this.auction = WebClient.builder().baseUrl(auctionBaseUrl).build();
    this.payment = WebClient.builder().baseUrl(paymentBaseUrl).build();
  }

  @GetMapping
  public String search(@RequestParam(name = "q", required = false) String q, Model model, HttpSession session) {
    if (session.getAttribute(SESSION_TOKEN) == null) {
      return "redirect:/ui/login";
    }
    try {
      List items = catalogue.get().uri(uriBuilder -> uriBuilder.path("/items/search").queryParam("q", q).build())
          .retrieve().bodyToFlux(Map.class).collectList().block();
      model.addAttribute("q", q == null ? "" : q);
      model.addAttribute("items", items);
      return "search";
    } catch (Exception ex) {
      model.addAttribute("message", "Catalogue service unavailable. Please start catalogue-service on port 8081.");
      return "error";
    }
  }

  @GetMapping("/items/{id}")
  public String item(@PathVariable("id") @Min(1) long id, Model model, HttpSession session) {
    if (session.getAttribute(SESSION_TOKEN) == null) {
      return "redirect:/ui/login";
    }
    try {
      Map item = catalogue.get().uri("/items/" + id)
          .exchangeToMono(resp -> {
            if (resp.statusCode().is2xxSuccessful()) {
              return resp.bodyToMono(Map.class);
            } else if (resp.statusCode().value() == 404) {
              return reactor.core.publisher.Mono.empty();
            } else {
              return resp.createException().flatMap(reactor.core.publisher.Mono::error);
            }
          }).block();
      if (item == null) {
        model.addAttribute("message", "Item not found. Use the Sell page to create a new item and start an auction.");
        return "error";
      }
      Map auc = auction.get().uri("/auctions/" + id).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
      model.addAttribute("item", item);
      model.addAttribute("auction", auc);
      Object locked = session.getAttribute(SESSION_LOCKED_ITEM);
      model.addAttribute("lockedItemId", locked);
      return "item";
    } catch (Exception ex) {
      model.addAttribute("message", "Catalogue or Auction service unavailable. Ensure 8081 and 8082 are running.");
      return "error";
    }
  }

  @PostMapping(value = "/auctions/{id}/bids", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String bid(@PathVariable("id") long id,
                    @RequestParam("bidderId") long bidderId,
                    @RequestParam("amount") int amount,
                    HttpSession session) {
    // enforce one-item-per-session bidding
    Object locked = session.getAttribute(SESSION_LOCKED_ITEM);
    if (locked == null) {
      session.setAttribute(SESSION_LOCKED_ITEM, id);
    } else if (!locked.toString().equals(String.valueOf(id))) {
      return "redirect:/ui/items/" + locked; // redirect to the first chosen item
    }
    auction.post().uri("/auctions/" + id + "/bids")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("bidderId", bidderId, "amount", amount))
        .retrieve().toBodilessEntity().onErrorResume(e -> {
          return auction.get().uri("/auctions/" + id).retrieve().toBodilessEntity();
        }).block();
    return "redirect:/ui/items/" + id;
  }

  @PostMapping(value = "/auctions/{id}/buy-now", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String buyNow(@PathVariable("id") long id,
                       @RequestParam("bidderId") long bidderId,
                       HttpSession session) {
    // enforce one-item-per-session bidding
    Object locked = session.getAttribute(SESSION_LOCKED_ITEM);
    if (locked == null) {
      session.setAttribute(SESSION_LOCKED_ITEM, id);
    } else if (!locked.toString().equals(String.valueOf(id))) {
      return "redirect:/ui/items/" + locked;
    }
    auction.post().uri("/auctions/" + id + "/buy-now")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(Map.of("bidderId", bidderId))
        .retrieve().toBodilessEntity().onErrorResume(e -> {
          return auction.get().uri("/auctions/" + id).retrieve().toBodilessEntity();
        }).block();
    return "redirect:/ui/ended/" + id;
  }

  @GetMapping("/ended/{id}")
  public String ended(@PathVariable("id") long id, Model model, HttpSession session) {
    if (session.getAttribute(SESSION_TOKEN) == null) {
      return "redirect:/ui/login";
    }
    try {
      Map auc = auction.get().uri("/auctions/" + id).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
      model.addAttribute("auction", auc);
      model.addAttribute("itemId", id);
      return "ended";
    } catch (Exception ex) {
      model.addAttribute("message", "Auction service unavailable. Please start auction-service on port 8082.");
      return "error";
    }
  }

  @GetMapping("/pay/{id}")
  public String payForm(@PathVariable("id") long id, Model model,
                        @RequestParam(name = "userId", required = false) Long userId,
                        @RequestParam(name = "expedited", required = false) Boolean expedited,
                        HttpSession session) {
    if (session.getAttribute(SESSION_TOKEN) == null) {
      return "redirect:/ui/login";
    }
    try {
      Map item = catalogue.get().uri("/items/" + id).retrieve().bodyToMono(Map.class).block();
      Map auc = auction.get().uri("/auctions/" + id).retrieve().bodyToMono(Map.class).onErrorReturn(Map.of()).block();
      model.addAttribute("item", item);
      model.addAttribute("auction", auc);
    } catch (Exception ex) {
      model.addAttribute("message", "Catalogue or Auction service unavailable. Please start services 8081 and 8082.");
      return "error";
    }
    // Use session user if available
    if (userId == null) {
      Object uid = session.getAttribute(SESSION_USER_ID);
      if (uid instanceof Long) userId = (Long) uid; else if (uid != null) userId = Long.valueOf(uid.toString());
    }
    model.addAttribute("userId", userId);
    model.addAttribute("expedited", expedited != null && expedited);
    // Prefill address from user profile if logged in
    Object token = session.getAttribute(SESSION_TOKEN);
    if (token != null) {
      try {
        Map me = WebClient.create("http://localhost:8084")
            .get().uri("/users/me")
            .headers(h -> h.setBearerAuth(token.toString()))
            .retrieve().bodyToMono(Map.class).block();
        if (me != null) {
          model.addAttribute("addressStreet", me.getOrDefault("street", ""));
          model.addAttribute("addressCity", me.getOrDefault("city", ""));
          model.addAttribute("addressCountry", me.getOrDefault("country", ""));
          model.addAttribute("postalCode", me.getOrDefault("postalCode", ""));
        }
      } catch (Exception ignore) {}
    }
    return "pay";
  }

  @PostMapping(value = "/pay/{id}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String pay(@PathVariable("id") long id,
                    @RequestParam("userId") long userId,
                    @RequestParam(name = "expedited", defaultValue = "false") boolean expedited,
                    @RequestParam("addressStreet") String addressStreet,
                    @RequestParam("addressCity") String addressCity,
                    @RequestParam("addressCountry") String addressCountry,
                    @RequestParam("postalCode") String postalCode,
                    @RequestParam("cardNumber") String cardNumber,
                    @RequestParam("cardName") String cardName,
                    @RequestParam("cardExpiry") String cardExpiry,
                    @RequestParam("cardCvv") String cardCvv,
                    Model model,
                    HttpSession session) {
    try {
      Map item = catalogue.get().uri("/items/" + id).retrieve().bodyToMono(Map.class).block();
      int shipRegular = ((Number) item.getOrDefault("shippingCostRegular", 0)).intValue();
      int shipExpedited = ((Number) item.getOrDefault("shippingCostExpedited", 0)).intValue();
      int shipping = expedited ? shipExpedited : shipRegular;

      Map auc = auction.get().uri("/auctions/" + id).retrieve().bodyToMono(Map.class).block();
      int price = ((Number) auc.getOrDefault("currentPrice", 0)).intValue();
      int amount = price + shipping;

      java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
      payload.put("itemId", id);
      payload.put("userId", userId);
      payload.put("amount", amount);
      payload.put("expeditedShipping", expedited);
      payload.put("addressStreet", addressStreet);
      payload.put("addressCity", addressCity);
      payload.put("addressCountry", addressCountry);
      payload.put("postalCode", postalCode);
      payload.put("cardNumber", cardNumber);
      payload.put("cardName", cardName);
      payload.put("cardExpiry", cardExpiry);
      payload.put("cardCvv", cardCvv);

      Map receipt = payment.post().uri("/payments")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(payload)
          .retrieve().bodyToMono(Map.class).block();

      model.addAttribute("receipt", receipt);
      return "receipt";
    } catch (Exception ex) {
      model.addAttribute("message", "Payment failed or dependent services unavailable. Ensure 8081, 8082, and 8083 are running.");
      return "error";
    }
  }

  @PostMapping(value = "/auctions/{id}/withdraw", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String withdraw(@PathVariable("id") long id, @RequestParam("bidderId") long bidderId) {
    auction.post().uri("/auctions/" + id + "/withdraw")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of("bidderId", bidderId))
        .retrieve().toBodilessEntity().onErrorResume(e -> auction.get().uri("/auctions/" + id).retrieve().toBodilessEntity())
        .block();
    return "redirect:/ui/items/" + id;
  }

  @PostMapping(value = "/auctions/{id}/decrease", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String decrease(@PathVariable("id") long id, @RequestParam("decrement") int decrement) {
    auction.post().uri("/auctions/" + id + "/decrease")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of("decrement", decrement))
        .retrieve().toBodilessEntity().onErrorResume(e -> auction.get().uri("/auctions/" + id).retrieve().toBodilessEntity())
        .block();
    return "redirect:/ui/items/" + id;
  }

  @GetMapping("/sell")
  public String sellForm(Model model, HttpSession session) {
    if (session.getAttribute(SESSION_TOKEN) == null) {
      return "redirect:/ui/login";
    }
    model.addAttribute("auctionTypes", java.util.List.of("FORWARD", "DUTCH"));
    model.addAttribute("conditions", java.util.List.of("NEW", "USED", "REFURBISHED"));
    return "sell";
  }

  @PostMapping(value = "/sell", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String sell(
      @RequestParam("name") String name,
      @RequestParam("description") String description,
      @RequestParam("category") String category,
      @RequestParam(name = "imageUrl", required = false) String imageUrl,
      @RequestParam("auctionType") String auctionType,
      @RequestParam("keywords") String keywords,
      @RequestParam("startingPrice") int startingPrice,
      @RequestParam(name = "reservePrice", required = false) Integer reservePrice,
      @RequestParam("shippingCostRegular") int shippingCostRegular,
      @RequestParam("shippingCostExpedited") int shippingCostExpedited,
      @RequestParam("shippingTimeDays") int shippingTimeDays,
      @RequestParam("condition") String condition,
      @RequestParam("quantity") int quantity,
      @RequestParam("durationSeconds") long durationSeconds,
      Model model
  ) {
    try {
      // Create item
      java.util.Map<String, Object> itemPayload = new java.util.LinkedHashMap<>();
      itemPayload.put("name", name);
      itemPayload.put("description", description);
      itemPayload.put("category", category);
      itemPayload.put("imageUrl", imageUrl);
      itemPayload.put("auctionType", auctionType);
      itemPayload.put("keywords", keywords);
      itemPayload.put("startingPrice", startingPrice);
      itemPayload.put("reservePrice", reservePrice);
      itemPayload.put("shippingCostRegular", shippingCostRegular);
      itemPayload.put("shippingCostExpedited", shippingCostExpedited);
      itemPayload.put("shippingTimeDays", shippingTimeDays);
      itemPayload.put("condition", condition);
      itemPayload.put("quantity", quantity);

      Map created = catalogue.post().uri("/items")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(itemPayload)
          .retrieve().bodyToMono(Map.class).block();
      long itemId = ((Number) created.get("id")).longValue();

      // Start auction
      java.util.Map<String, Object> startPayload = new java.util.LinkedHashMap<>();
      startPayload.put("itemId", itemId);
      startPayload.put("type", auctionType);
      startPayload.put("startingPrice", startingPrice);
      startPayload.put("durationSeconds", durationSeconds);
      startPayload.put("reservePrice", reservePrice);
      auction.post().uri("/auctions/start")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(startPayload)
          .retrieve().toBodilessEntity().block();

      return "redirect:/ui/items/" + itemId;
    } catch (Exception ex) {
      model.addAttribute("message", "Unable to create item or start auction. Ensure catalogue (8081) and auction (8082) are running.");
      return "error";
    }
  }

  // --- Auth pages ---
  @GetMapping("/login")
  public String loginForm() { return "login"; }

  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String login(@RequestParam("username") String username,
                      @RequestParam("password") String password,
                      HttpSession session) {
    Map res = WebClient.create("http://localhost:8084")
        .post().uri("/users/sign-in")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of("username", username, "password", password))
        .exchangeToMono(resp -> {
          if (resp.statusCode().is2xxSuccessful()) {
            return resp.bodyToMono(Map.class);
          } else if (resp.statusCode().value() == 401) {
            return reactor.core.publisher.Mono.just(java.util.Collections.emptyMap());
          } else {
            return resp.createException().flatMap(reactor.core.publisher.Mono::error);
          }
        }).block();
    if (res != null && res.get("token") != null) {
      session.setAttribute(SESSION_TOKEN, res.get("token"));
      session.setAttribute(SESSION_USER_ID, ((Number)res.get("userId")).longValue());
      return "redirect:/ui";
    }
    // invalid credentials -> show friendly message on login page
    return "redirect:/ui/login?error=1";
  }

  @GetMapping("/sign-up")
  public String signUpForm() { return "signup"; }

  @PostMapping(value = "/sign-up", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String signUp(@RequestParam("username") String username,
                       @RequestParam("password") String password,
                       @RequestParam("firstName") String firstName,
                       @RequestParam("lastName") String lastName,
                       @RequestParam("street") String street,
                       @RequestParam("city") String city,
                       @RequestParam("country") String country,
                       @RequestParam("postalCode") String postalCode,
                       HttpSession session) {
    WebClient.create("http://localhost:8084")
        .post().uri("/users/sign-up")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of(
            "username", username,
            "password", password,
            "firstName", firstName,
            "lastName", lastName,
            "street", street,
            "city", city,
            "country", country,
            "postalCode", postalCode
        ))
        .retrieve().toBodilessEntity().block();
    // auto-login
    return login(username, password, session);
  }

  @GetMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/ui/login";
  }

  @GetMapping("/forgot")
  public String forgotForm() { return "forgot"; }

  @PostMapping(value = "/forgot", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String forgot(@RequestParam("username") String username, Model model) {
    Map res = WebClient.create("http://localhost:8084")
        .post().uri("/users/forgot-password")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of("username", username))
        .retrieve().bodyToMono(Map.class).block();
    model.addAttribute("info", "If the user exists, a reset token has been issued.");
    model.addAttribute("resetToken", res != null ? res.get("resetToken") : null);
    return "forgot";
  }

  @GetMapping("/reset")
  public String resetForm() { return "reset"; }

  @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public String reset(@RequestParam("token") String token, @RequestParam("newPassword") String newPassword) {
    WebClient.create("http://localhost:8084")
        .post().uri("/users/reset-password")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(java.util.Map.of("token", token, "newPassword", newPassword))
        .retrieve().toBodilessEntity().block();
    return "redirect:/ui/login";
  }
}
