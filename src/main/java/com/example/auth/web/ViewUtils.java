package com.example.auth.web;

public final class ViewUtils {
  private ViewUtils() {}

  public static String fmtDuration(Long seconds) {
    if (seconds == null) return "—";
    long s = Math.max(0, seconds);
    long h = s / 3600; s %= 3600;
    long m = s / 60; s %= 60;
    if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
    return String.format("%d:%02d", m, s);
  }
}


