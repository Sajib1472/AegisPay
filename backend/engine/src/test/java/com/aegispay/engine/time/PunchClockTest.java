package com.aegispay.engine.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PunchClockTest {

    @Test
    void oneMinuteDropsSeconds() {
        Instant raw = Instant.parse("2024-06-03T15:00:41Z");
        assertEquals(Instant.parse("2024-06-03T15:00:00Z"), PunchClock.round(raw, 1));
    }

    @Test
    void fifteenMinutesRoundsToNearestBlock() {
        Instant raw = Instant.parse("2024-06-03T15:07:00Z");
        assertEquals(Instant.parse("2024-06-03T15:00:00Z"), PunchClock.round(raw, 15));
    }
}
