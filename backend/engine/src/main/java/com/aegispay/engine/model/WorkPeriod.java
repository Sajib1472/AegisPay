package com.aegispay.engine.model;

import com.aegispay.engine.money.Money;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public record WorkPeriod(
        String employeeId,
        ExemptionStatus exemptionStatus,
        DayOfWeek workweekStart,
        List<Shift> shifts,
        List<Bonus> bonuses,
        List<Attestation> attestations,
        TenantPolicy policy
) {
    public WorkPeriod {
        shifts = List.copyOf(shifts);
        bonuses = List.copyOf(bonuses);
        attestations = List.copyOf(attestations);
    }

    public enum ExemptionStatus {
        NON_EXEMPT,
        EXEMPT_SALARY
    }

    public enum IntervalType {
        WORK,
        PAID_BREAK,
        UNPAID_MEAL,
        TRAVEL,
        ON_CALL
    }

    public enum RateType {
        HOURLY,
        DIFFERENTIAL_FLAT,
        DIFFERENTIAL_PERCENT
    }

    public record TenantPolicy(
            int punchRoundMinutes,
            boolean mealWaiverUnderSixHours,
            boolean autoRestPremium,
            boolean attestationOverridesClock
    ) {
        public static TenantPolicy conservativeDefaults() {
            return new TenantPolicy(1, true, true, true);
        }
    }

    public record PayRate(
            String jobCode,
            RateType type,
            Money hourly
    ) {
    }

    public record Interval(
            Instant start,
            Instant end,
            IntervalType type,
            String locationId,
            String jobCode
    ) {
        public Interval {
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Interval end must be after start");
            }
        }
    }

    public record Shift(
            String locationId,
            String jobCode,
            ZoneId timeZone,
            List<String> jurisdictions,
            List<Interval> intervals,
            List<PayRate> rates
    ) {
        public Shift {
            jurisdictions = List.copyOf(jurisdictions);
            intervals = List.copyOf(intervals);
            rates = List.copyOf(rates);
        }

        public boolean hasJurisdiction(String code) {
            return jurisdictions.contains(code);
        }
    }

    public record Bonus(
            Money amount,
            LocalDate earnedOn,
            boolean discretionary,
            String note
    ) {
    }

    public record Attestation(
            LocalDate workDate,
            MealAnswer meal,
            MealAnswer rest
    ) {
        public enum MealAnswer {
            YES,
            NO,
            WAIVED,
            UNKNOWN
        }
    }
}
