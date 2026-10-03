package com.aegispay.engine.leave;

import com.aegispay.engine.money.Hours;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * CA Healthy Workplaces Healthy Families Act: 1 hour per 30 hours worked.
 * Cap is tenant/city policy. Ledger in the app is the source of truth.
 */
public final class LeaveAccrualCalculator {

    public static final String CA_SICK = "CA_SICK";

    private LeaveAccrualCalculator() {
    }

    public static Hours accrue(Hours hoursWorked, Hours currentBalance, Policy policy) {
        if (hoursWorked == null || hoursWorked.isZero() || hoursWorked.isNegative()) {
            return Hours.ZERO;
        }
        Hours cap = policy.capHours();
        Hours room = cap.minus(currentBalance == null ? Hours.ZERO : currentBalance);
        if (room.isZero() || room.isNegative()) {
            return Hours.ZERO;
        }
        BigDecimal earned = hoursWorked.toBigDecimal()
                .divide(BigDecimal.valueOf(policy.hoursWorkedPerAccruedHour()), 4, RoundingMode.HALF_UP);
        return Hours.of(earned).min(room);
    }

    public static Policy resolve(List<String> jurisdictions) {
        List<String> codes = jurisdictions == null ? List.of() : jurisdictions;
        boolean city = codes.stream().anyMatch(c -> c.contains("LOS-ANGELES") || c.contains("SANTA-MONICA")
                || c.contains("OAKLAND") || c.contains("SF") || c.contains("SAN-FRANCISCO"));
        return city ? Policy.cityOverlay() : Policy.caStatewide();
    }

    public record Policy(String code, int hoursWorkedPerAccruedHour, Hours capHours) {
        public static Policy caStatewide() {
            return new Policy(CA_SICK, 30, Hours.of("40"));
        }

        public static Policy cityOverlay() {
            return new Policy(CA_SICK, 30, Hours.of("48"));
        }
    }
}
