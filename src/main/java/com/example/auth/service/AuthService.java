package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
  private final UserRepository repo;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository repo) {
    this.repo = repo;
  }

  public User register(String username, String rawPassword, String firstName, String lastName,
      String streetName, String streetNumber, String city, String country, String postalCode, boolean enable2fa) {
    if (repo.findByUsername(username).isPresent()) {
      throw new IllegalArgumentException("Username already taken");
    }
    String hash = encoder.encode(rawPassword);

    String twofaSecret = enable2fa ? TotpUtil.generateBase32Secret(20) : null;

    repo.create(username, hash, firstName, lastName, streetName, streetNumber, city, country, postalCode, twofaSecret);

    return repo.findByUsername(username).orElseThrow();
  }

  public Optional<User> authenticate(String username, String rawPassword) {
    return repo.findByUsername(username)
        .filter(u -> encoder.matches(rawPassword, u.getPasswordHash()));
  }

  public boolean requiresOtp(User u) {
    return u.getTwofaSecret() != null && !u.getTwofaSecret().isEmpty();
  }

  public boolean verifyOtp(User u, String otpCode) {
    if (!requiresOtp(u))
      return true;
    return TotpUtil.validateCode(u.getTwofaSecret(), otpCode, 1);
  }

  public Optional<User> findByUsername(String username) {
    return repo.findByUsername(username);
  }

  public void updatePassword(User u, String rawPassword) {
    String hash = encoder.encode(rawPassword);

    repo.updatePassword(u.getUsername(), hash);
  }

  public boolean verifyPostal(User u, String postal) {
    if (postal == null)
      return false;

    String a = normalizePostal(u.getPostalCode());

    String b = normalizePostal(postal);

    return !a.isEmpty() && a.equals(b);
  }

  private String normalizePostal(String p) {
    return p == null ? "" : p.replaceAll("\\s", "").toUpperCase();
  }
}

