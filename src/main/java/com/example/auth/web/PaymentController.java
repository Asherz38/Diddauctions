package com.example.auth.web;

import com.example.auth.repo.BidRepository;
import com.example.auth.repo.ItemRepository;
import com.example.auth.repo.PaymentRepository;
import com.example.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Value;

@Controller
public class PaymentController {
  private final ItemRepository items;

  private final BidRepository bids;

  private final PaymentRepository payments;

  private final AuthService authService;

  @Value("${app.payment.acceptAnyCard:true}")
  private boolean acceptAnyCard;

  public PaymentController(ItemRepository items, BidRepository bids, PaymentRepository payments, AuthService authService) {
    this.items = items; 
    this.bids = bids; 
    this.payments = payments; 
    this.authService = authService; 
  }

  @GetMapping("/pay")
  public String pay(@RequestParam("id") Integer id, HttpSession session, Model model) {
    if (session.getAttribute("uid") == null) return "redirect:/auth";

    var item = items.findById(id);

    if (item == null) 
    return "redirect:/catalog";

    var top = bids.topBid(id);
    Object username = session.getAttribute("username");

    boolean isWinner = top.isPresent() && username != null && username.equals(top.get().getUsername());

    model.addAttribute("item", item);
    
    model.addAttribute("isWinner", isWinner);

    model.addAttribute("shipping", item.getShippingPrice());

    model.addAttribute("expedite", item.getExpeditePrice());

    
    if (username != null) {
      authService.findByUsername(username.toString()).ifPresent(u -> model.addAttribute("addr", u));
    }
    return "pay";
  }

  @PostMapping("/pay")
  public String doPay(@RequestParam("id") Integer id,@RequestParam(name = "expedite", required = false) String expedite, @RequestParam(name = "cardNumber", required = false) String cardNumber,@RequestParam(name = "nameOnCard", required = false) String nameOnCard, @RequestParam(name = "expMonth", required = false) String expMonth,@RequestParam(name = "expYear", required = false) String expYear, @RequestParam(name = "cvc", required = false) String cvc, HttpSession session, Model model) {
    if (session.getAttribute("uid") == null) return "redirect:/auth";

    var item = items.findById(id);

    if (item == null) return "redirect:/catalog";
    var top = bids.topBid(id);

    Object username = session.getAttribute("username");

    boolean isWinner = top.isPresent() && username != null && username.equals(top.get().getUsername());

    if (!isWinner) {
      model.addAttribute("error", "Only the winning bidder can pay.");

      model.addAttribute("item", item);

      model.addAttribute("isWinner", false);

      model.addAttribute("shipping", item.getShippingPrice());

      model.addAttribute("expedite", item.getExpeditePrice());

      return "pay";
    }

    
    String validation = validatePayment(cardNumber, nameOnCard, expMonth, expYear, cvc);
    if (validation != null) {
      model.addAttribute("error", validation);

      model.addAttribute("item", item);

      model.addAttribute("isWinner", true);

      model.addAttribute("shipping", item.getShippingPrice());

      model.addAttribute("expedite", item.getExpeditePrice());

      return "pay";
    }
    double winning = item.getCurrentPrice() != null ? item.getCurrentPrice() : item.getStartingPrice();

    double shipping = item.getShippingPrice() != null ? item.getShippingPrice() : 0.0;

    double extra = (expedite != null) ? (item.getExpeditePrice() != null ? item.getExpeditePrice() : 0.0) : 0.0;

    double total = winning + shipping + extra;
    
    int userId = (Integer) session.getAttribute("uid");

    long receiptId = payments.record(id, userId, total, expedite != null);

    model.addAttribute("item", item);

    model.addAttribute("total", total);

    model.addAttribute("winning", winning);

    model.addAttribute("shipping", shipping);

    model.addAttribute("extra", extra);

    model.addAttribute("receiptId", receiptId);

    
    Object uname = session.getAttribute("username");

    if (uname != null) {
      authService.findByUsername(uname.toString()).ifPresent(u -> model.addAttribute("addr", u));
    }
    model.addAttribute("shipDays", item.getShippingDays() != null ? item.getShippingDays() : 5);
    return "paid";
  }

  private String digitsOnly(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }

  private String validatePayment(String cardNumber, String nameOnCard, String expMonth, String expYear, String cvc) {
    String num = digitsOnly(cardNumber);
    if (num.length() < 13 || num.length() > 19)
      return "Invalid card number length";

    if (!acceptAnyCard && !luhn(num))
      return "Invalid card number. This demo only accepts valid test numbers such as 4242 4242 4242 4242 (Visa), 5555 5555 5555 4444 (MC), or 3782 822463 10005 (Amex).";

    if (nameOnCard == null || nameOnCard.trim().length() < 2) return "Name on card is required";
    int mm, yy;

    try { mm = Integer.parseInt(expMonth); } catch (Exception e) { 
      return "Invalid expiry month"; 
    }
    
    try {
       yy = Integer.parseInt(expYear); } catch (Exception e) { 
        return "Invalid expiry year"; 
      }
    if (mm < 1 || mm > 12) 
    return "Invalid expiry month";

    if (yy < 100) yy += 2000; 
    java.time.YearMonth exp = java.time.YearMonth.of(yy, mm);

    if (exp.isBefore(java.time.YearMonth.now())) return "Card expired";

    String c = digitsOnly(cvc);
    if (c.length() < 3 || c.length() > 4) 
    return "Invalid security code";

    return null;
  }

  private boolean luhn(String num) {
    int sum = 0; boolean alt = false;
    for (int i = num.length()-1; i >= 0; i--) {
      int n = num.charAt(i) - '0';
      if (alt) { 
        n *= 2; if (n > 9) n -= 9; 
      }
      sum += n; alt = !alt;
    }
    return sum % 10 == 0;
  }
}

