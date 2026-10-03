package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Interval;
import com.aegispay.engine.model.WorkPeriod.IntervalType;
import com.aegispay.engine.model.WorkPeriod.PayRate;
import com.aegispay.engine.model.WorkPeriod.RateType;
import com.aegispay.engine.model.WorkPeriod.Shift;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns intervals into per-local-date worked hours, split by job/rate.
 */
public final class WorkedTime {

    private WorkedTime() {
    }

    public record RateSlice(String jobCode, Money hourly, Hours hours) {
    }

    public record DayWork(
            LocalDate date,
            ZoneId zone,
            List<String> jurisdictions,
            List<RateSlice> slices,
            Hours worked,
            Hours unpaidMeal,
            boolean hadUnpaidMealInterval
    ) {
        public boolean hasJurisdiction(String code) {
            return jurisdictions.contains(code);
        }

        public Money weightedStraightPay() {
            Money total = Money.ZERO;
            for (RateSlice slice : slices) {
                total = total.plus(slice.hourly().times(slice.hours()));
            }
            return total;
        }
    }

    public static List<DayWork> byLocalDate(WorkPeriod period) {
        Map<LocalDate, MutableDay> days = new LinkedHashMap<>();
        for (Shift shift : period.shifts()) {
            for (Interval interval : shift.intervals()) {
                LocalDate date = interval.start().atZone(shift.timeZone()).toLocalDate();
                MutableDay day = days.computeIfAbsent(date, d -> new MutableDay(d, shift));
                Hours hours = Hours.fromDuration(Duration.between(interval.start(), interval.end()));
                String job = interval.jobCode() == null ? shift.jobCode() : interval.jobCode();
                Money hourly = rateFor(shift, job);
                if (interval.type() == IntervalType.WORK
                        || interval.type() == IntervalType.PAID_BREAK
                        || interval.type() == IntervalType.TRAVEL) {
                    day.addWork(job, hourly, hours);
                } else if (interval.type() == IntervalType.UNPAID_MEAL) {
                    day.unpaidMeal = day.unpaidMeal.plus(hours);
                    day.hadUnpaidMealInterval = true;
                }
            }
        }
        List<DayWork> result = new ArrayList<>();
        for (MutableDay day : days.values()) {
            result.add(day.toDayWork());
        }
        return result;
    }

    private static Money rateFor(Shift shift, String jobCode) {
        return shift.rates().stream()
                .filter(r -> r.type() == RateType.HOURLY && (jobCode == null || jobCode.equals(r.jobCode())))
                .map(PayRate::hourly)
                .findFirst()
                .orElseGet(() -> primaryHourly(shift));
    }

    private static Money primaryHourly(Shift shift) {
        return shift.rates().stream()
                .filter(r -> r.type() == RateType.HOURLY)
                .map(PayRate::hourly)
                .findFirst()
                .orElse(Money.ZERO);
    }

    private static final class MutableDay {
        private final LocalDate date;
        private final ZoneId zone;
        private final List<String> jurisdictions;
        private final Map<String, RateSliceAcc> slices = new LinkedHashMap<>();
        private Hours unpaidMeal = Hours.ZERO;
        private boolean hadUnpaidMealInterval;

        private MutableDay(LocalDate date, Shift shift) {
            this.date = date;
            this.zone = shift.timeZone();
            this.jurisdictions = shift.jurisdictions();
        }

        private void addWork(String jobCode, Money hourly, Hours hours) {
            slices.computeIfAbsent(jobCode + "|" + hourly, k -> new RateSliceAcc(jobCode, hourly))
                    .hours = slices.get(jobCode + "|" + hourly).hours.plus(hours);
        }

        private DayWork toDayWork() {
            List<RateSlice> list = new ArrayList<>();
            Hours worked = Hours.ZERO;
            for (RateSliceAcc acc : slices.values()) {
                list.add(new RateSlice(acc.jobCode, acc.hourly, acc.hours));
                worked = worked.plus(acc.hours);
            }
            return new DayWork(date, zone, jurisdictions, list, worked, unpaidMeal, hadUnpaidMealInterval);
        }
    }

    private static final class RateSliceAcc {
        private final String jobCode;
        private final Money hourly;
        private Hours hours = Hours.ZERO;

        private RateSliceAcc(String jobCode, Money hourly) {
            this.jobCode = jobCode;
            this.hourly = hourly;
        }
    }
}
