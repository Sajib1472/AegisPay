package com.aegispay.engine;

import com.aegispay.engine.calc.CaliforniaRules;
import com.aegispay.engine.calc.FlsaOvertimeCalculator;
import com.aegispay.engine.calc.FlsaOvertimeCalculator.WeekBuckets;
import com.aegispay.engine.calc.RegularRateCalculator;
import com.aegispay.engine.calc.WorkedTime;
import com.aegispay.engine.calc.WorkedTime.DayWork;
import com.aegispay.engine.calc.WorkedTime.RateSlice;
import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Attestation;
import com.aegispay.engine.model.WorkPeriod.ExemptionStatus;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;
import com.aegispay.engine.result.EarningsResult.PayException;
import com.aegispay.engine.rules.RulePack;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Deterministic wage-and-hour engine. No Spring. Same inputs + same packs = same output.
 */
public final class AegisPayEngine {

    public EarningsResult calculate(WorkPeriod period, List<RulePack> packs, EngineOptions options) {
        EarningsResult.Builder builder = EarningsResult.builder(period.employeeId(), EngineVersion.VALUE);
        for (RulePack pack : packs) {
            builder.rulePack(pack.jurisdiction() + "@" + pack.version());
        }

        if (period.exemptionStatus() == ExemptionStatus.EXEMPT_SALARY) {
            builder.warning("Employee is marked exempt. Overtime and break premiums were not calculated.");
            return builder.build();
        }

        List<DayWork> days = WorkedTime.byLocalDate(period);
        days.sort(Comparator.comparing(DayWork::date));

        Hours totalWorked = Hours.ZERO;
        Money straightEarnings = Money.ZERO;
        Hours dailyRegular = Hours.ZERO;
        Hours dailyOt = Hours.ZERO;
        Hours dailyDt = Hours.ZERO;
        boolean useCalifornia = options.includeCalifornia();

        for (DayWork day : days) {
            boolean caDay = useCalifornia && day.hasJurisdiction("US-CA");
            Hours worked = day.worked();
            totalWorked = totalWorked.plus(worked);
            straightEarnings = straightEarnings.plus(day.weightedStraightPay());

            if (caDay) {
                CaliforniaRules.DailySplit split = CaliforniaRules.splitDaily(worked);
                dailyRegular = dailyRegular.plus(split.regular());
                dailyOt = dailyOt.plus(split.ot15());
                dailyDt = dailyDt.plus(split.doubleTime());
            } else {
                dailyRegular = dailyRegular.plus(worked);
            }
        }

        WeekBuckets week = FlsaOvertimeCalculator.applyWeeklyOvertime(dailyRegular, dailyOt, dailyDt);
        Money regularRate = RegularRateCalculator.calculate(
                totalWorked,
                straightEarnings,
                period.bonuses()
        );
        builder.regularRate(regularRate);

        Money displayStraight = averageStraightRate(days, totalWorked);
        LocalDate weekEnding = days.isEmpty() ? LocalDate.EPOCH : days.get(days.size() - 1).date();

        for (EarningsLine line : FlsaOvertimeCalculator.toLines(
                weekEnding,
                week.regular(),
                week.ot15(),
                week.doubleTime(),
                displayStraight,
                regularRate.equals(Money.ZERO) ? displayStraight : regularRate
        )) {
            builder.line(line);
        }

        Money bonusTotal = RegularRateCalculator.nonDiscretionaryBonusTotal(period);
        if (!bonusTotal.isZero()) {
            builder.line(new EarningsLine(
                    weekEnding,
                    EarningBucket.BONUS,
                    Hours.ZERO,
                    Money.ZERO,
                    bonusTotal,
                    new Explanation(
                            "BONUS_ND",
                            "29 CFR 778.209",
                            Map.of("amount", bonusTotal.toString()),
                            bonusTotal.toString() + " included in regular rate",
                            "Non-discretionary bonus paid this period and included in the regular rate."
                    )
            ));
            if (week.ot15().isGreaterThan(Hours.ZERO) || week.doubleTime().isGreaterThan(Hours.ZERO)) {
                builder.warning("Regular rate includes a non-discretionary bonus true-up for overtime.");
            }
        }

        Money premiumRate = regularRate.equals(Money.ZERO) ? displayStraight : regularRate;
        for (DayWork day : days) {
            if (!useCalifornia || !day.hasJurisdiction("US-CA")) {
                continue;
            }
            Optional<Attestation> attestation = period.attestations().stream()
                    .filter(a -> a.workDate().equals(day.date()))
                    .findFirst();
            boolean meal = CaliforniaRules.mealViolation(day, period.policy(), attestation);
            boolean rest = CaliforniaRules.restViolation(day, attestation, period.policy());
            for (EarningsLine premium : CaliforniaRules.premiums(day, premiumRate, meal, rest)) {
                builder.line(premium);
            }
            if (meal) {
                builder.exception(CaliforniaRules.mealException(day.date()));
            }
            if (rest) {
                builder.exception(CaliforniaRules.restException(day.date()));
            }
        }

        if (totalWorked.isGreaterThan(Hours.of(60))) {
            builder.exception(new PayException(
                    "HIGH_HOURS",
                    "WARNING",
                    false,
                    weekEnding,
                    "More than 60 hours in the workweek. Confirm punches before approval."
            ));
        }

        EarningsResult result = builder.build();
        assertNonNegative(result);
        return result;
    }

    private static Money averageStraightRate(List<DayWork> days, Hours totalWorked) {
        if (totalWorked.isZero()) {
            return Money.ZERO;
        }
        Money pay = Money.ZERO;
        for (DayWork day : days) {
            for (RateSlice slice : day.slices()) {
                pay = pay.plus(slice.hourly().times(slice.hours()));
            }
        }
        return RegularRateCalculator.calculate(totalWorked, pay, List.of());
    }

    private static void assertNonNegative(EarningsResult result) {
        if (result.totals().gross().isNegative()) {
            throw new IllegalStateException("Engine produced negative gross");
        }
        for (var line : result.lines()) {
            if (line.hours().isNegative() || line.amount().isNegative()) {
                throw new IllegalStateException("Engine produced a negative earnings line");
            }
        }
    }
}
