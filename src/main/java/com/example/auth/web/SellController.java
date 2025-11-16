package com.example.auth.web;

import com.example.auth.repo.ItemRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SellController {
  private final ItemRepository items;

  public SellController(ItemRepository items) {
    this.items = items;
  }

  @GetMapping("/sell")
  public String sellForm(HttpSession session, Model model) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";
    return "sell";
  }

  @PostMapping("/sell")
  public String create(@RequestParam String name,
      @RequestParam String description, @RequestParam String auctionType, @RequestParam double startingPrice,
      @RequestParam(name = "duration", required = false) Integer duration,
      @RequestParam(name = "unit", required = false) String unit,
      @RequestParam(name = "shippingPrice", required = false) Double shippingPrice,
      @RequestParam(name = "expeditePrice", required = false) Double expeditePrice,
      @RequestParam(name = "shippingDays", required = false) Integer shippingDays,
      HttpSession session, RedirectAttributes ra) {

    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    if (name == null || name.trim().isEmpty()) {
      ra.addFlashAttribute("error", "Item name is required");
      return "redirect:/sell";
    }

    String type = auctionType != null ? auctionType.toUpperCase() : "FORWARD";

    if (!type.equals("FORWARD") && !type.equals("DUTCH")) {
      ra.addFlashAttribute("error", "Invalid auction type");
      return "redirect:/sell";
    }

    if (startingPrice < 0) {
      ra.addFlashAttribute("error", "Starting price must be >= 0");
      return "redirect:/sell";
    }

    String closesAt = null;
    if (type.equals("FORWARD")) {
      if (duration == null || duration <= 0) {
        ra.addFlashAttribute("error", "Duration is required for forward auctions");
        return "redirect:/sell";
      }

      long minutes = switch (unit == null ? "hours" : unit.toLowerCase()) {
        case "minutes" -> duration;
        case "days" -> duration * 24L * 60L;
        default -> duration * 60L; 
      };

      java.time.ZonedDateTime end = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusMinutes(minutes);

      closesAt = end.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    }

    Integer sellerId = (Integer) session.getAttribute("uid");
    long id = items.create(name.trim(), description == null ? "" : description.trim(), type,startingPrice, startingPrice, "ACTIVE", closesAt, shippingPrice, expeditePrice, shippingDays, sellerId);

    if (id <= 0) {
      ra.addFlashAttribute("error", "Could not create item");
      return "redirect:/sell";
    }

    ra.addFlashAttribute("msg", "Auction created successfully");

    return "redirect:/catalog";
  }

  @GetMapping("/sell/manage")
  public String manage(HttpSession session, Model model) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";
    int uid = (Integer) session.getAttribute("uid");
    model.addAttribute("items", items.listDutchBySeller(uid));
    return "sell-manage";
  }

  @PostMapping("/sell/dutch/price")
  public String updateDutch(@RequestParam int id,@RequestParam double newPrice, HttpSession session,
  RedirectAttributes ra) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";
    int uid = (Integer) session.getAttribute("uid");
    if (newPrice < 0) {
      ra.addFlashAttribute("error", "Price must be non-negative");
      return "redirect:/sell/manage";
    }
    int updated = items.updateDutchPrice(id, uid, newPrice);
    if (updated == 0) {
      ra.addFlashAttribute("error", "Update failed. Ensure this is your active Dutch auction and new price is lower.");
    } else {
      ra.addFlashAttribute("msg", "Price updated to $" + newPrice);
    }
    return "redirect:/sell/manage";
  }
}

