package ca.yorku.itec4020.user.web;

import ca.yorku.itec4020.user.entity.UserEntity;
import ca.yorku.itec4020.user.service.TokenService;
import ca.yorku.itec4020.user.service.PasswordResetService;
import ca.yorku.itec4020.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/users")
public class UserController {
  private final UserService userService;
  private final TokenService tokenService;
  private final PasswordResetService passwordResetService;

  public UserController(UserService userService, TokenService tokenService, PasswordResetService passwordResetService) {
    this.userService = userService;
    this.tokenService = tokenService;
    this.passwordResetService = passwordResetService;
  }

  public record SignUpRequest(@NotBlank String username, @NotBlank String password,
                              String firstName, String lastName,
                              String street, String city, String country, String postalCode) {}

  @PostMapping("/sign-up")
  public ResponseEntity<?> signUp(@Valid @RequestBody SignUpRequest req) {
    UserEntity u = new UserEntity();
    u.setUsername(req.username());
    u.setPassword(req.password());
    u.setFirstName(req.firstName());
    u.setLastName(req.lastName());
    u.setStreet(req.street());
    u.setCity(req.city());
    u.setCountry(req.country());
    u.setPostalCode(req.postalCode());
    u = userService.signUp(u);
    return ResponseEntity.ok(Map.of("userId", u.getId()));
  }

  public record SignInRequest(@NotBlank String username, @NotBlank String password) {}

  @PostMapping("/sign-in")
  public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest req) {
    return userService.signIn(req.username(), req.password())
        .map(u -> ResponseEntity.ok(Map.of(
            "token", tokenService.issueToken(u.getId()),
            "userId", u.getId()
        )))
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid credentials")));
  }

  @GetMapping("/me")
  public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) {
      return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
    }
    String token = auth.substring("Bearer ".length());
    return tokenService.validate(token)
        .flatMap(userService::getById)
        .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of(
            "userId", u.getId(),
            "username", u.getUsername(),
            "firstName", u.getFirstName(),
            "lastName", u.getLastName(),
            "street", u.getStreet(),
            "city", u.getCity(),
            "country", u.getCountry(),
            "postalCode", u.getPostalCode()
        )))
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token")));
  }

  public record UpdateProfileRequest(String firstName, String lastName,
                                     String street, String city, String country, String postalCode) {}

  @PutMapping("/me")
  public ResponseEntity<?> updateProfile(@RequestHeader(name = "Authorization", required = false) String auth,
                                         @RequestBody @Valid UpdateProfileRequest req) {
    if (auth == null || !auth.startsWith("Bearer ")) {
      return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
    }
    String token = auth.substring("Bearer ".length());
    return tokenService.validate(token)
        .flatMap(userService::getById)
        .map(u -> {
          u.setFirstName(req.firstName());
          u.setLastName(req.lastName());
          u.setStreet(req.street());
          u.setCity(req.city());
          u.setCountry(req.country());
          u.setPostalCode(req.postalCode());
          userService.save(u);
          return ResponseEntity.ok(Map.of("status", "updated"));
        })
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token")));
  }

  public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}

  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(@RequestHeader(name = "Authorization", required = false) String auth,
                                          @RequestBody @Valid ChangePasswordRequest req) {
    if (auth == null || !auth.startsWith("Bearer ")) {
      return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
    }
    String token = auth.substring("Bearer ".length());
    return tokenService.validate(token)
        .flatMap(userService::getById)
        .map(u -> {
          if (!u.getPassword().equals(req.oldPassword())) {
            return ResponseEntity.status(400).body(Map.of("error", "Old password does not match"));
          }
          u.setPassword(req.newPassword());
          userService.save(u);
          return ResponseEntity.ok(Map.of("status", "password-changed"));
        })
        .orElseGet(() -> ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token")));
  }

  public record ForgotPasswordRequest(@NotBlank String username) {}

  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest req) {
    var userOpt = userService.signIn(req.username(), "").or(() -> userService.getByUsername(req.username()));
    if (userOpt.isEmpty()) {
      // avoid user enumeration: return success even if not found
      return ResponseEntity.ok(Map.of("status", "ok"));
    }
    long userId = userOpt.get().getId();
    String resetToken = passwordResetService.issueToken(userId, 15 * 60); // 15 minutes TTL
    // For demo purposes we return the token; in real systems we'd email it.
    return ResponseEntity.ok(Map.of("resetToken", resetToken));
  }

  public record ResetPasswordRequest(@NotBlank String token, @NotBlank String newPassword) {}

  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest req) {
    return passwordResetService.consumeToken(req.token())
        .flatMap(userService::getById)
        .map(u -> {
          u.setPassword(req.newPassword());
          userService.save(u);
          return ResponseEntity.ok(Map.of("status", "password-reset"));
        })
        .orElseGet(() -> ResponseEntity.status(400).body(Map.of("error", "Invalid or expired reset token")));
  }
}
