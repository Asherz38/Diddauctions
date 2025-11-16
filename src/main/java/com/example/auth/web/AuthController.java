package com.example.auth.web;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.service.TotpUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
  private final AuthService auth;

  public AuthController(AuthService auth) {
    this.auth = auth;
  }

  @GetMapping({ "/", "/auth" })
  public String authPage(Model model, HttpSession session,
      @RequestParam(name = "msg", required = false) String msg) {
    if (session.getAttribute("uid") != null)
      return "redirect:/dashboard";
    if (msg != null)
      model.addAttribute("msg", msg);
    return "auth";
  }

  @GetMapping("/auth/forgot")
  public String forgotPage() {
    return "forgot";
  }

  @PostMapping("/auth/signup")
  public String signup(@RequestParam(required = false) String username, @RequestParam(required = false) String password,
      @RequestParam(required = false) String confirm, @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName, @RequestParam(required = false) String streetName,
      @RequestParam(required = false) String streetNumber, @RequestParam(required = false) String city,
      @RequestParam(required = false) String country, @RequestParam(required = false) String postalCode,
      @RequestParam(name = "enable2fa", required = false) String enable2fa, RedirectAttributes ra,
      HttpSession session) {
    
    if (isBlank(username) || isBlank(password) || isBlank(confirm) || isBlank(firstName) || isBlank(lastName)
        || isBlank(streetName) || isBlank(streetNumber) || isBlank(city) || isBlank(country) || isBlank(postalCode)) {
      ra.addFlashAttribute("error", "All fields are required");
      return "redirect:/auth";
    }

    if (!password.equals(confirm)) {
      ra.addFlashAttribute("error", "Passwords do not match");
      return "redirect:/auth";
    }
    if (password.length() < 8) {
      ra.addFlashAttribute("error", "Password must be at least 8 characters");
      return "redirect:/auth";
    }
    try {
      boolean enable = enable2fa != null;
      User u = auth.register(
          username.trim().toLowerCase(), password,
          firstName.trim(), lastName.trim(),
          streetName.trim(), streetNumber.trim(),
          city.trim(), country.trim(), postalCode.trim(),
          enable);
      session.setAttribute("uid", u.getId());
      session.setAttribute("username", u.getUsername());
      session.setAttribute("name", u.getFirstName() + " " + u.getLastName());
      if (enable && u.getTwofaSecret() != null) {
        session.setAttribute("twofa_secret_once", u.getTwofaSecret());
        return "redirect:/auth/2fa-setup";
      }
      return "redirect:/dashboard";
    } catch (IllegalArgumentException ex) {
      ra.addFlashAttribute("error", ex.getMessage());
      return "redirect:/auth";
    } catch (org.springframework.dao.DataAccessException ex) {
      ra.addFlashAttribute("error", "Database error: " + ex.getClass().getSimpleName());
      return "redirect:/auth";
    }
  }

  @PostMapping("/auth/signin")
  public String signin(@RequestParam String username, @RequestParam String password,
      @RequestParam(name = "code", required = false) String code, RedirectAttributes ra, HttpSession session) {
    return auth.authenticate(username.trim().toLowerCase(), password)
        .map(u -> {
          if (auth.requiresOtp(u)) {
            if (code == null || code.isBlank()) {
              ra.addFlashAttribute("error", "OTP code required (2FA enabled)");

              return "redirect:/auth";
            }
            if (!auth.verifyOtp(u, code)) {
              ra.addFlashAttribute("error", "Invalid OTP code");
              return "redirect:/auth";
            }
          }
          session.setAttribute("uid", u.getId());
          session.setAttribute("username", u.getUsername());
          session.setAttribute("name", u.getFirstName() + " " + u.getLastName());
          return "redirect:/dashboard";
        })
        .orElseGet(() -> {
          ra.addFlashAttribute("error", "Invalid username or password");
          return "redirect:/auth";
        });
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  @GetMapping("/auth/2fa-setup")
  public String twofaSetup(HttpSession session, Model model) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    String secret = (String) session.getAttribute("twofa_secret_once");

    if (secret == null)
      return "redirect:/dashboard";

    String username = (String) session.getAttribute("username");

    String uri = TotpUtil.provisioningUri("AuthApp", username, secret);
    model.addAttribute("secret", secret);
    model.addAttribute("otpauth", uri);
    try {
      String qr = "https://chart.googleapis.com/chart?chs=220x220&cht=qr&chl=" +
                 java.net.URLEncoder.encode(uri, java.nio.charset.StandardCharsets.UTF_8);
      model.addAttribute("qrUrl", qr);
    } catch (Exception ignored) {}

    session.removeAttribute("twofa_secret_once"); 

    return "twofa-setup";
  }

  @GetMapping("/dashboard")
  public String dashboard(HttpSession session, Model model) {
    if (session.getAttribute("uid") == null)
      return "redirect:/auth";

    model.addAttribute("name", session.getAttribute("name"));

    model.addAttribute("username", session.getAttribute("username"));

    return "dashboard";
  }

  @GetMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();

    return "redirect:/auth?msg=Signed out";
  }

  @PostMapping("/auth/forgot")
  public String doForgot(@RequestParam(required = false) String username, @RequestParam(required = false) String postal,
      @RequestParam(required = false) String code, @RequestParam(required = false) String newPassword,
      @RequestParam(required = false) String confirm, RedirectAttributes ra) {
    if (isBlank(username) || isBlank(newPassword) || isBlank(confirm)) {
      ra.addFlashAttribute("error", "Username and new password are required");

      return "redirect:/auth/forgot";
    }
    if (!newPassword.equals(confirm)) {
      ra.addFlashAttribute("error", "Passwords do not match");

      return "redirect:/auth/forgot";
    }
    if (newPassword.length() < 8) {
      ra.addFlashAttribute("error", "Password must be at least 8 characters");

      return "redirect:/auth/forgot";
    }
    return auth.findByUsername(username.trim().toLowerCase())
        .map(u -> {
          if (auth.requiresOtp(u)) {
            if (isBlank(code) || !auth.verifyOtp(u, code)) {
              ra.addFlashAttribute("error", "Invalid or missing OTP code");

              return "redirect:/auth/forgot";
            }
          } else {
            if (isBlank(postal) || !auth.verifyPostal(u, postal)) {
              ra.addFlashAttribute("error", "Postal code does not match");

              return "redirect:/auth/forgot";
            }
          }
          auth.updatePassword(u, newPassword);
          ra.addFlashAttribute("msg", "Password updated. Please sign in.");

          return "redirect:/auth";
        })
        .orElseGet(() -> {
          ra.addFlashAttribute("error", "User not found");

          return "redirect:/auth/forgot";
        });
  }
}



