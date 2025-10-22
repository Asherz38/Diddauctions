package ca.yorku.itec4020.user.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
  private static class Entry { long userId; Instant expiresAt; }
  private final Map<String, Entry> tokens = new ConcurrentHashMap<>();
  private final long ttlSeconds = 30 * 60; // 30 minutes

  public String issueToken(long userId) {
    String token = UUID.randomUUID().toString();
    Entry e = new Entry();
    e.userId = userId;
    e.expiresAt = Instant.now().plusSeconds(ttlSeconds);
    tokens.put(token, e);
    return token;
  }

  public Optional<Long> validate(String token) {
    Entry e = tokens.get(token);
    if (e == null) return Optional.empty();
    if (Instant.now().isAfter(e.expiresAt)) {
      tokens.remove(token);
      return Optional.empty();
    }
    return Optional.of(e.userId);
  }
}
