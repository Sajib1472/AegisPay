package com.aegispay.engine;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.ExemptionStatus;
import com.aegispay.engine.model.WorkPeriod.Interval;
import com.aegispay.engine.model.WorkPeriod.IntervalType;
import com.aegispay.engine.model.WorkPeriod.PayRate;
import com.aegispay.engine.model.WorkPeriod.RateType;
import com.aegispay.engine.model.WorkPeriod.Shift;
import com.aegispay.engine.model.WorkPeriod.TenantPolicy;
import com.aegispay.engine.result.EarningsResult;
import com.aegispay.engine.rules.PublishedRulePack;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnginePropertyAndCitationTest {

    private final AegisPayEngine engine = new AegisPayEngine();

    @Test
    void engineVersionIsPipelined() {
        assertEquals("0.2.0", EngineVersion.VALUE);
        assertEquals(12, EvaluationPipeline.values().length);
    }

    @Test
    void hoursAndAmountsNeverNegative() {
        WorkPeriod period = week(8);
        EarningsResult result = engine.calculate(period, List.of(PublishedRulePack.flsa()), EngineOptions.fromPacks(List.of(PublishedRulePack.flsa())));
        assertFalse(result.totals().gross().isNegative());
        result.lines().forEach(l -> {
            assertFalse(l.hours().isNegative());
            assertFalse(l.amount().isNegative());
        });
    }

    @Test
    void dailyBucketsDoNotExceedWorkedHoursExceptPremiums() {
        WorkPeriod period = week(8);
        EarningsResult result = engine.calculate(period, List.of(PublishedRulePack.flsa()), EngineOptions.fromPacks(List.of(PublishedRulePack.flsa())));
        Hours paidHours = result.totals().regularHours().plus(result.totals().otHours()).plus(result.totals().doubleTimeHours());
        assertTrue(!paidHours.isGreaterThan(Hours.of(40)) || result.totals().otHours().isGreaterThan(Hours.ZERO) || paidHours.equals(Hours.of(40)));
    }

    @Test
    void dualRateUsesWeightedStraightPay() {
        Interval morning = new Interval(
                LocalDate.of(2024, 6, 3).atTime(8, 0).toInstant(ZoneOffset.UTC),
                LocalDate.of(2024, 6, 3).atTime(12, 0).toInstant(ZoneOffset.UTC),
                IntervalType.WORK, "loc-1", "RDH");
        Interval afternoon = new Interval(
                LocalDate.of(2024, 6, 3).atTime(13, 0).toInstant(ZoneOffset.UTC),
                LocalDate.of(2024, 6, 3).atTime(17, 0).toInstant(ZoneOffset.UTC),
                IntervalType.WORK, "loc-1", "FRONT");
        Shift shift = new Shift("loc-1", "RDH", ZoneOffset.UTC, List.of("US-FLSA"), List.of(morning, afternoon),
                List.of(new PayRate("RDH", RateType.HOURLY, Money.of("40.00")),
                        new PayRate("FRONT", RateType.HOURLY, Money.of("20.00"))));
        WorkPeriod period = new WorkPeriod("hygienist", ExemptionStatus.NON_EXEMPT, DayOfWeek.SUNDAY,
                List.of(shift), List.of(), List.of(), TenantPolicy.conservativeDefaults());
        EarningsResult result = engine.calculate(period, List.of(PublishedRulePack.flsa()), EngineOptions.fromPacks(List.of(PublishedRulePack.flsa())));
        assertEquals(Money.of("240.00"), result.totals().regularPay());
    }

    private static WorkPeriod week(int hoursPerDay) {
        List<Shift> shifts = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            LocalDate date = LocalDate.of(2024, 6, 3).plusDays(d);
            Interval interval = new Interval(
                    date.atTime(8, 0).toInstant(ZoneOffset.UTC),
                    date.atTime(8, 0).plusHours(hoursPerDay).toInstant(ZoneOffset.UTC),
                    IntervalType.WORK, "loc", "RDH");
            shifts.add(new Shift("loc", "RDH", ZoneOffset.UTC, List.of("US-FLSA"), List.of(interval),
                    List.of(new PayRate("RDH", RateType.HOURLY, Money.of("10.00")))));
        }
        return new WorkPeriod("p", ExemptionStatus.NON_EXEMPT, DayOfWeek.SUNDAY, shifts, List.of(), List.of(), TenantPolicy.conservativeDefaults());
    }
}
