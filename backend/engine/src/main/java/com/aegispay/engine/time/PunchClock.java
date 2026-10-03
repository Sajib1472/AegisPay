package com.aegispay.engine.time;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Default 1 minute = compliance honesty. 5 or 15 only if the tenant policy says so.
 */
public final class PunchClock {

    private PunchClock() {
    }

    public static Instant round(Instant instant, int minutes) {
        if (minutes <= 1) {
            return instant.truncatedTo(ChronoUnit.MINUTES);
        }
        long epochMinutes = instant.getEpochSecond() / 60;
        long rounded = Math.round(epochMinutes / (double) minutes) * minutes;
        return Instant.ofEpochSecond(rounded * 60);
    }
}
