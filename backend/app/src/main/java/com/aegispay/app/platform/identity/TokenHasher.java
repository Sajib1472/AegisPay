package com.aegispay.app.platform.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

public final class TokenHasher {

    private TokenHasher() {
    }

    public static String randomToken() {
        return UUID.randomUUID() + UUID.randomUUID().toString().replace("-", "");
    }

    public static String sha256(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
