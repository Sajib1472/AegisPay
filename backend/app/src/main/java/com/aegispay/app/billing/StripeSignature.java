package com.aegispay.app.billing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Verifies Stripe-Signature without the Stripe SDK: t=unix,v1=hmac_sha256(secret, t + "." + payload).
 */
public final class StripeSignature {

    private StripeSignature() {
    }

    public static void verify(String header, String payload, String secret, long nowEpochSeconds, long toleranceSeconds) {
        if (header == null || header.isBlank() || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe signature");
        }
        String timestamp = null;
        String v1 = null;
        for (String part : header.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0].trim())) {
                timestamp = kv[1].trim();
            } else if ("v1".equals(kv[0].trim())) {
                v1 = kv[1].trim();
            }
        }
        if (timestamp == null || v1 == null) {
            throw new IllegalArgumentException("Stripe-Signature missing t or v1");
        }
        long t = Long.parseLong(timestamp);
        if (Math.abs(nowEpochSeconds - t) > toleranceSeconds) {
            throw new IllegalArgumentException("Stripe signature timestamp too old");
        }
        String expected = hmac(secret, timestamp + "." + payload);
        if (!constantTimeEquals(expected, v1)) {
            throw new IllegalArgumentException("Stripe signature mismatch");
        }
    }

    static String hmac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length(); i++) {
            r |= a.charAt(i) ^ b.charAt(i);
        }
        return r == 0;
    }

    public static Instant now() {
        return Instant.now();
    }
}
