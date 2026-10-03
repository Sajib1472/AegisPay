package com.aegispay.app.time;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record PunchCursor(Instant adjustedAt, UUID id) {

    public static PunchCursor parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String decoded = new String(Base64.getUrlDecoder().decode(raw), StandardCharsets.UTF_8);
        String[] parts = decoded.split("\\|", 2);
        return new PunchCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
    }

    public String encode() {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((adjustedAt + "|" + id).getBytes(StandardCharsets.UTF_8));
    }
}
