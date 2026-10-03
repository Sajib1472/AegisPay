package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Attestation;
import com.aegispay.engine.model.WorkPeriod.Attestation.MealAnswer;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;
import com.aegispay.engine.result.EarningsResult.PayException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * California daily overtime, double-time, meal and rest premiums.
 * Clinics typically fall under IWC Wage Order 4 — tenant must confirm the wage order.
 */
public final class CaliforniaRules {

    public static final Hours DAILY_OT_AFTER = Hours.of(8);
    public static final Hours DAILY_DT_AFTER = Hours.of(12);
    public static final Hours MEAL_BY_HOURS = Hours.of(5);
    public static final Hours SECOND_MEAL_BY_HOURS = Hours.of(10);
    public static final Hours MEAL_MINIMUM = Hours.of("0.5");

    private CaliforniaRules() {
    }

    public record DailySplit(Hours regular, Hours ot15, Hours doubleTime) {
    }

    public static DailySplit splitDaily(Hours worked) {
        Hours regular = worked.min(DAILY_OT_AFTER);
        Hours afterEight = worked.minus(regular);
        Hours ot15 = afterEight.min(Hours.of(4)); // 8–12
        Hours doubleTime = worked.isGreaterThan(DAILY_DT_AFTER) ? worked.minus(DAILY_DT_AFTER) : Hours.ZERO;
        if (doubleTime.isGreaterThan(Hours.ZERO)) {
            ot15 = Hours.of(4);
        }
        return new DailySplit(regular, ot15, doubleTime);
    }

    public static boolean mealViolation(
            WorkedTime.DayWork day,
            WorkPeriod.TenantPolicy policy,
            Optional<Attestation> attestation
    ) {
        if (!day.worked().isGreaterThan(MEAL_BY_HOURS)
                && policy.mealWaiverUnderSixHours()
                && !day.worked().isGreaterThan(Hours.of(6))) {
            if (attestation.map(a -> a.meal() == MealAnswer.WAIVED).orElse(false)) {
                return false;
            }
        }
        if (attestation.isPresent() && policy.attestationOverridesClock()) {
            MealAnswer answer = attestation.get().meal();
            if (answer == MealAnswer.NO) {
                return true;
            }
            if (answer == MealAnswer.YES || answer == MealAnswer.WAIVED) {
                return false;
            }
        }
        if (!day.worked().isGreaterThan(MEAL_BY_HOURS)) {
            return false;
        }
        if (day.hadUnpaidMealInterval() && !day.unpaidMeal().isGreaterThan(MEAL_MINIMUM.minus(Hours.of("0.0001")))
                && day.unpaidMeal().isGreaterThan(Hours.of("0.4999"))) {
            return false;
        }
        return !day.hadUnpaidMealInterval() || day.unpaidMeal().isGreaterThan(Hours.ZERO)
                && !day.unpaidMeal().isGreaterThan(Hours.of("0.4999"));
    }

    public static boolean restViolation(WorkedTime.DayWork day, Optional<Attestation> attestation, WorkPeriod.TenantPolicy policy) {
        if (!policy.autoRestPremium()) {
            return attestation.map(a -> a.rest() == MealAnswer.NO).orElse(false);
        }
        if (attestation.isPresent() && policy.attestationOverridesClock() && attestation.get().rest() == MealAnswer.NO) {
            return true;
        }
        if (attestation.map(a -> a.rest() == MealAnswer.YES).orElse(false)) {
            return false;
        }
        return day.worked().isGreaterThan(Hours.of("3.5"));
    }

    public static List<EarningsLine> premiums(
            WorkedTime.DayWork day,
            Money regularRate,
            boolean meal,
            boolean rest
    ) {
        List<EarningsLine> lines = new ArrayList<>();
        if (meal) {
            lines.add(premiumLine(day.date(), EarningBucket.MEAL_PREMIUM, "CA_MEAL_PREMIUM",
                    "Cal. Lab. Code § 226.7; IWC Wage Order 4 § 11",
                    regularRate,
                    "Missed, late, or short meal break: one additional hour at the regular rate."));
        }
        if (rest) {
            lines.add(premiumLine(day.date(), EarningBucket.REST_PREMIUM, "CA_REST_PREMIUM",
                    "Cal. Lab. Code § 226.7; IWC Wage Order 4 § 12",
                    regularRate,
                    "Rest break not provided: one additional hour at the regular rate."));
        }
        return lines;
    }

    private static EarningsLine premiumLine(
            LocalDate date,
            EarningBucket bucket,
            String code,
            String citation,
            Money regularRate,
            String narrative
    ) {
        Hours one = Hours.of(1);
        return new EarningsLine(
                date,
                bucket,
                one,
                regularRate,
                regularRate.times(one),
                new Explanation(
                        code,
                        citation,
                        Map.of("regularRate", regularRate.toString(), "hours", "1"),
                        "1 × $" + regularRate,
                        narrative
                )
        );
    }

    public static PayException mealException(LocalDate date) {
        return new PayException(
                "MEAL_PREMIUM",
                "WARNING",
                false,
                date,
                "California meal-break premium generated. Confirm punches and attestation before approval."
        );
    }

    public static PayException restException(LocalDate date) {
        return new PayException(
                "REST_PREMIUM",
                "WARNING",
                false,
                date,
                "California rest-break premium generated."
        );
    }

    public static Hours seventhDayThreshold() {
        return Hours.of(8);
    }

    public static BigDecimal timeAndHalf() {
        return BigDecimal.valueOf(1.5);
    }
}
