package com.example.auth.web;

import com.example.auth.repo.BidRepository;
import com.example.auth.repo.ItemRepository;
import com.example.auth.model.Item;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class CatalogController {
  private final ItemRepository items;

  private final BidRepository bids;

  public CatalogController(ItemRepository items, BidRepository bids) {
    this.items = items;

    this.bids = bids;
  }

  @GetMapping("/catalog")
  public String catalog(@RequestParam(name = "q", required = false) String q,
      HttpSession session, Model model) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    
    items.closeExpired();

    model.addAttribute("q", q == null ? "" : q);

    model.addAttribute("items", (q == null || q.isBlank()) ? items.listActive() : items.search(q));

    Object selectedId = session.getAttribute("selectedItemId");

    if (selectedId != null) {
      model.addAttribute("selectedItemId", selectedId);

      model.addAttribute("selectedItemName", session.getAttribute("selectedItemName"));

    }
    return "catalog";
  }

  @GetMapping("/catalog/bid")
  public String bid(@RequestParam(name = "id") Integer id,
      HttpSession session, Model model,
      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    if (id == null)
      return "redirect:/catalog";
    items.closeExpired();

    Item item = items.findById(id);

    if (item == null || "CLOSED".equals(item.getStatus()))
      return "redirect:/catalog";

    Object existing = session.getAttribute("selectedItemId");
    if (existing == null) {
      session.setAttribute("selectedItemId", item.getId());

      session.setAttribute("selectedItemName", item.getName());

    } else {
      try {
        int e = (existing instanceof Integer) ? (Integer) existing : Integer.parseInt(existing.toString());

        if (e != item.getId()) {
          ra.addFlashAttribute("error", "You have already selected '" + session.getAttribute("selectedItemName")
              + "' for this session. Sign out to change selection.");

          return "redirect:/catalog";
        }
      } catch (NumberFormatException ignored) {
      }
    }
    model.addAttribute("item", item);
    bids.topBid(item.getId()).ifPresent(top -> {
      model.addAttribute("topAmount", top.getAmount());

      model.addAttribute("topUser", top.getUsername());

    });
    return "bid";
  }

  @GetMapping("/auction/ended")
  public String ended(@RequestParam("id") Integer id, HttpSession session, Model model) {
    var item = items.findById(id);

    if (item == null)
      return "redirect:/catalog";

    var top = bids.topBid(id);
    model.addAttribute("item", item);

    model.addAttribute("finalPrice", item.getCurrentPrice());

    model.addAttribute("winner", top.map(t -> t.getUsername()).orElse(null));

    Object uid = session.getAttribute("uid");

    boolean isWinner = false;

    if (uid != null && top.isPresent()) {
      
      Object username = session.getAttribute("username");

      isWinner = username != null && username.equals(top.get().getUsername());
    }
    model.addAttribute("isWinner", isWinner);
    return "ended";
  }

  @PostMapping("/catalog/bid")
  public String placeBid(@RequestParam("id") Integer id,
      @RequestParam("amount") Double amount,
      HttpSession session,
      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    if (id == null || amount == null)
      return "redirect:/catalog";

    items.closeExpired();
    var item = items.findById(id);
    if (item == null)
      return "redirect:/catalog";

    if (!"FORWARD".equals(item.getAuctionType()) || "CLOSED".equals(item.getStatus())) {
      ra.addFlashAttribute("error", "Bidding not allowed for this item");

      return "redirect:/catalog";
    }

    Object sel = session.getAttribute("selectedItemId");

    if (sel == null || Integer.parseInt(sel.toString()) != id) {
      ra.addFlashAttribute("error", "Select this item first on the catalogue page");

      return "redirect:/catalog";
    }

    double current = item.getCurrentPrice() != null ? item.getCurrentPrice() : item.getStartingPrice();

    if (amount <= current) {
      ra.addFlashAttribute("error", "Bid must be higher than current price ($" + current + ")");

      return "redirect:/catalog/bid?id=" + id;
    }
    int userId = (Integer) session.getAttribute("uid");

    bids.placeBid(id, userId, amount);
    return "redirect:/catalog/bid?id=" + id;
  }

  @PostMapping("/catalog/buy-now")
  public String buyNow(@RequestParam("id") Integer id,
      HttpSession session,
      org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    if (id == null)
      return "redirect:/catalog";

    items.closeExpired();
    var item = items.findById(id);
    if (item == null)
      return "redirect:/catalog";

    if (!"DUTCH".equals(item.getAuctionType()) || "CLOSED".equals(item.getStatus())) {
      ra.addFlashAttribute("error", "Buy Now not available for this item");
      return "redirect:/catalog";
    }
    Object sel = session.getAttribute("selectedItemId");
    if (sel == null || Integer.parseInt(sel.toString()) != id) {
      ra.addFlashAttribute("error", "Select this item first on the catalogue page");
      return "redirect:/catalog";
    }
    int userId = (Integer) session.getAttribute("uid");
    bids.buyNow(id, userId);
    return "redirect:/auction/ended?id=" + id;
  }

  @GetMapping("/catalog/bid/state")
  @ResponseBody
  public java.util.Map<String, Object> bidState(@RequestParam("id") Integer id) {
    items.closeExpired();

    var item = items.findById(id);

    var map = new java.util.HashMap<String, Object>();

    if (item == null)
      return map;

    map.put("status", item.getStatus());

    map.put("currentPrice", item.getCurrentPrice());

    map.put("remainingSeconds", item.getRemainingSeconds());

    bids.topBid(item.getId()).ifPresent(top -> {
      map.put("topAmount", top.getAmount());
      map.put("topUser", top.getUsername());
    });
    return map;
  }
}

