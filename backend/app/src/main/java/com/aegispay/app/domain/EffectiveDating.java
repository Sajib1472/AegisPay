package com.aegispay.app.domain;

import java.time.LocalDate;

/**
 * A raise on Wednesday must not rewrite Monday’s rate.
 */
public final class EffectiveDating {

    private EffectiveDating() {
    }

    public static boolean covers(LocalDate effectiveFrom, LocalDate effectiveTo, LocalDate day) {
        if (effectiveFrom == null || day == null || day.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !day.isAfter(effectiveTo);
    }
}
