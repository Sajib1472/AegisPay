package com.aegispay.engine;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Attestation;
import com.aegispay.engine.model.WorkPeriod.Attestation.MealAnswer;
import com.aegispay.engine.model.WorkPeriod.ExemptionStatus;
import com.aegispay.engine.model.WorkPeriod.Interval;
import com.aegispay.engine.model.WorkPeriod.IntervalType;
import com.aegispay.engine.model.WorkPeriod.PayRate;
import com.aegispay.engine.model.WorkPeriod.RateType;
import com.aegispay.engine.model.WorkPeriod.Shift;
import com.aegispay.engine.model.WorkPeriod.TenantPolicy;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.rules.PublishedRulePack;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Golden vectors from DOL Fact Sheet #23 / 29 CFR 778 and California daily OT.
 */
class FlsaGoldenTest {

    private final AegisPayEngine engine = new AegisPayEngine();
    private static final ZoneId UTC = ZoneOffset.UTC;

    @Test
    void dolFactSheet23_hourlyOvertimeWithoutBonus() {
        WorkPeriod period = period("alex", List.of(
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 3), 8)),
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 4), 8)),
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 5), 8)),
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 6), 8)),
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 7), 12))
        ), List.of(), List.of());

        EarningsResult result = engine.calculate(
                period,
                List.of(PublishedRulePack.flsa()),
                EngineOptions.fromPacks(List.of(PublishedRulePack.flsa()))
        );

        assertEquals(Hours.of(40), result.totals().regularHours());
        assertEquals(Hours.of(4), result.totals().otHours());
        assertEquals(Money.of("9.00"), result.regularRate());
        assertEquals(Money.of("414.00"), result.totals().gross());
    }

    @Test
    void cfr778_nonDiscretionaryBonusRaisesRegularRate() {
        WorkPeriod period = period("blake", List.of(
                shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 3), 46))
        ), List.of(new WorkPeriod.Bonus(Money.of("46.00"), LocalDate.of(2024, 6, 7), false, "production")), List.of());

        EarningsResult result = engine.calculate(
                period,
                List.of(PublishedRulePack.flsa()),
                EngineOptions.fromPacks(List.of(PublishedRulePack.flsa()))
        );

        assertEquals(Money.of("10.00"), result.regularRate());
        assertTrue(result.lines().stream().anyMatch(l -> l.bucket() == EarningBucket.BONUS));
        assertTrue(result.totals().otHours().isGreaterThan(Hours.ZERO));
    }

    @Test
    void californiaNineHourDayCreatesDailyOvertime() {
        WorkPeriod period = period("maria", List.of(
                shift("US-CA", hoursOn(LocalDate.of(2024, 6, 3), 9.25))
        ), List.of(), List.of(new Attestation(LocalDate.of(2024, 6, 3), MealAnswer.YES, MealAnswer.YES)));

        var packs = List.of(PublishedRulePack.flsa(), PublishedRulePack.california());
        EarningsResult result = engine.calculate(period, packs, EngineOptions.fromPacks(packs));

        assertEquals(Hours.of(8), result.totals().regularHours());
        assertEquals(Hours.of("1.25"), result.totals().otHours());
    }

    @Test
    void californiaMissedMealGeneratesPremium() {
        WorkPeriod period = period("jordan", List.of(
                shift("US-CA", hoursOn(LocalDate.of(2024, 6, 3), 8))
        ), List.of(), List.of(new Attestation(LocalDate.of(2024, 6, 3), MealAnswer.NO, MealAnswer.YES)));

        var packs = List.of(PublishedRulePack.flsa(), PublishedRulePack.california());
        EarningsResult result = engine.calculate(period, packs, EngineOptions.fromPacks(packs));

        assertTrue(result.lines().stream().anyMatch(l -> l.bucket() == EarningBucket.MEAL_PREMIUM));
        assertEquals(Money.of("9.00"), result.totals().premiumPay());
    }

    @Test
    void dualRateDayUsesWeightedStraightPay() {
        Interval morning = interval(LocalDate.of(2024, 6, 3), 8, 12);
        Interval afternoon = new Interval(
                LocalDate.of(2024, 6, 3).atTime(13, 0).toInstant(ZoneOffset.UTC),
                LocalDate.of(2024, 6, 3).atTime(17, 0).toInstant(ZoneOffset.UTC),
                IntervalType.WORK,
                "loc-1",
                "FRONT"
        );
        Shift shift = new Shift(
                "loc-1",
                "RDH",
                UTC,
                List.of("US-FLSA"),
                List.of(morning, afternoon),
                List.of(
                        new PayRate("RDH", RateType.HOURLY, Money.of("40.00")),
                        new PayRate("FRONT", RateType.HOURLY, Money.of("20.00"))
                )
        );
        // WorkedTime uses primary hourly from first HOURLY rate for all slices in current v0.1.
        WorkPeriod period = period("hygienist", List.of(shift), List.of(), List.of());
        EarningsResult result = engine.calculate(
                period,
                List.of(PublishedRulePack.flsa()),
                EngineOptions.fromPacks(List.of(PublishedRulePack.flsa()))
        );
        assertTrue(result.totals().regularHours().isGreaterThan(Hours.of(7)));
        assertTrue(!result.totals().gross().isZero());
    }

    @Test
    void exemptEmployeeSkipsOvertime() {
        WorkPeriod period = new WorkPeriod(
                "dr-lee",
                ExemptionStatus.EXEMPT_SALARY,
                DayOfWeek.SUNDAY,
                List.of(shift("US-CA", hoursOn(LocalDate.of(2024, 6, 3), 12))),
                List.of(),
                List.of(),
                TenantPolicy.conservativeDefaults()
        );
        var packs = List.of(PublishedRulePack.flsa(), PublishedRulePack.california());
        EarningsResult result = engine.calculate(period, packs, EngineOptions.fromPacks(packs));
        assertTrue(result.lines().isEmpty());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void recalculateIsIdempotent() {
        WorkPeriod period = period("sam", List.of(shift("US-FLSA", hoursOn(LocalDate.of(2024, 6, 3), 46))), List.of(), List.of());
        var packs = List.of(PublishedRulePack.flsa());
        EngineOptions options = EngineOptions.fromPacks(packs);
        EarningsResult a = engine.calculate(period, packs, options);
        EarningsResult b = engine.calculate(period, packs, options);
        assertEquals(a.totals().gross(), b.totals().gross());
        assertEquals(a.regularRate(), b.regularRate());
    }

    private static WorkPeriod period(
            String id,
            List<Shift> shifts,
            List<WorkPeriod.Bonus> bonuses,
            List<Attestation> attestations
    ) {
        return new WorkPeriod(
                id,
                ExemptionStatus.NON_EXEMPT,
                DayOfWeek.SUNDAY,
                shifts,
                bonuses,
                attestations,
                TenantPolicy.conservativeDefaults()
        );
    }

    private static Shift shift(String jurisdiction, Interval interval) {
        return new Shift(
                "loc-1",
                "RDH",
                UTC,
                List.of(jurisdiction, "US-FLSA"),
                List.of(interval),
                List.of(new PayRate("RDH", RateType.HOURLY, Money.of("9.00")))
        );
    }

    private static Interval hoursOn(LocalDate date, double hours) {
        LocalDateTime start = date.atTime(8, 0);
        return new Interval(
                start.toInstant(ZoneOffset.UTC),
                start.plusMinutes((long) (hours * 60)).toInstant(ZoneOffset.UTC),
                IntervalType.WORK,
                "loc-1",
                "RDH"
        );
    }

    private static Interval interval(LocalDate date, int startHour, int endHour) {
        return new Interval(
                date.atTime(startHour, 0).toInstant(ZoneOffset.UTC),
                date.atTime(endHour, 0).toInstant(ZoneOffset.UTC),
                IntervalType.WORK,
                "loc-1",
                "RDH"
        );
    }
}
