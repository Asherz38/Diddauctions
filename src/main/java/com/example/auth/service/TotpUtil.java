package com.example.auth.service;

import org.apache.commons.codec.binary.Base32;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;



public final class TotpUtil {
  private TotpUtil() {
  }

  public static String generateBase32Secret(int bytes) {
    byte[] buffer = new byte[bytes];

    new SecureRandom().nextBytes(buffer);

    return new Base32().encodeToString(buffer).replace("=", "");

  }

  public static boolean validateCode(String base32Secret, String code, int window) {
    if (base32Secret == null || base32Secret.isEmpty())
      return false;

    if (code == null || !code.matches("\\d{6}"))
      return false;

    long timestep = System.currentTimeMillis() / 1000L / 30L;

    for (int i = -window; i <= window; i++) {
      String expected = totpAt(base32Secret, timestep + i, 6);

      if (expected.equals(code))
        return true;

    }
    return false;
  }

  public static String provisioningUri(String issuer, String username, String base32Secret) {
    String label = urlEncode(issuer) + ":" + urlEncode(username);

    String params = "secret=" + base32Secret + "&issuer=" + urlEncode(issuer) + "&algorithm=SHA1&digits=6&period=30";

    return "otpauth://totp/" + label + "?" + params;
  }

  private static String totpAt(String base32Secret, long timestep, int digits) {
    byte[] key = new Base32().decode(base32Secret);

    byte[] data = ByteBuffer.allocate(8).putLong(timestep).array();

    try {
      Mac mac = Mac.getInstance("HmacSHA1");

      mac.init(new SecretKeySpec(key, "HmacSHA1"));

      byte[] hash = mac.doFinal(data);

      int offset = hash[hash.length - 1] & 0x0F;
      int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16) |
          ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

      int otp = binary % (int) Math.pow(10, digits);

      return String.format("%0" + digits + "d", otp);

    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
  }

  private static String urlEncode(String s) {
    try {
      return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);

    } catch (Exception e) {
      return s;
    }
  }
}

