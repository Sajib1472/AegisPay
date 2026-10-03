package com.aegispay.app.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PunchAdjustmentServiceTest {

    @Test
    void snapshotCapturesOriginalAndAdjusted() {
        Punch punch = new Punch();
        punch.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        punch.setPersonId(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        punch.setLocationId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        punch.setPunchType("IN");
        punch.setSource("CSV");
        punch.setOriginalAt(Instant.parse("2024-06-03T15:00:00Z"));
        punch.setAdjustedAt(Instant.parse("2024-06-03T15:00:00Z"));
        Map<String, Object> row = PunchAdjustmentService.snapshot(punch);
        assertEquals("IN", row.get("punchType"));
        assertEquals("2024-06-03T15:00:00Z", row.get("originalAt"));
        assertNull(row.get("voidedAt"));
    }
}
