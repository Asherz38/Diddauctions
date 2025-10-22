package ca.yorku.itec4020.user.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {
  private static class Entry {
    final long userId;
    final Instant expiresAt;
    Entry(long userId, Instant expiresAt) { this.userId = userId; this.expiresAt = expiresAt; }
  }

  private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

  public String issueToken(long userId, long ttlSeconds) {
    String token = UUID.randomUUID().toString();
    tokens.put(token, new Entry(userId, Instant.now().plusSeconds(ttlSeconds)));
    return token;
  }

  public Optional<Long> consumeToken(String token) {
    Entry e = tokens.remove(token);
    if (e == null) return Optional.empty();
    if (Instant.now().isAfter(e.expiresAt)) return Optional.empty();
    return Optional.of(e.userId);
  }
}
