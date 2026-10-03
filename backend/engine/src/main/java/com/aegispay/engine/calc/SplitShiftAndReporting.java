package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod.Interval;
import com.aegispay.engine.model.WorkPeriod.IntervalType;
import com.aegispay.engine.model.WorkPeriod.Shift;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class SplitShiftAndReporting {

    private SplitShiftAndReporting() {
    }

    public static List<EarningsLine> premiums(Shift shift, Money regularRate, boolean california) {
        if (!california) {
            return List.of();
        }
        List<Interval> work = shift.intervals().stream()
                .filter(i -> i.type() == IntervalType.WORK)
                .sorted(Comparator.comparing(Interval::start))
                .toList();
        List<EarningsLine> lines = new ArrayList<>();
        if (work.size() >= 2) {
            for (int i = 1; i < work.size(); i++) {
                Duration gap = Duration.between(work.get(i - 1).end(), work.get(i).start());
                if (gap.toMinutes() >= 60) {
                    LocalDate date = work.get(i).start().atZone(shift.timeZone()).toLocalDate();
                    lines.add(new EarningsLine(
                            date,
                            EarningBucket.SPLIT_SHIFT,
                            Hours.of(1),
                            regularRate,
                            regularRate,
                            new Explanation(
                                    "CA_SPLIT_SHIFT",
                                    "IWC Wage Order 4 § 4 (split-shift)",
                                    Map.of("gapMinutes", String.valueOf(gap.toMinutes())),
                                    "1 × $" + regularRate,
                                    "Split-shift premium: gap of at least one hour between work intervals."
                            )
                    ));
                    break;
                }
            }
        }
        Hours worked = Hours.ZERO;
        for (Interval interval : work) {
            worked = worked.plus(Hours.fromDuration(Duration.between(interval.start(), interval.end())));
        }
        if (!work.isEmpty() && !worked.isGreaterThan(Hours.of(2))) {
            LocalDate date = work.get(0).start().atZone(shift.timeZone()).toLocalDate();
            lines.add(new EarningsLine(
                    date,
                    EarningBucket.REPORTING_TIME,
                    Hours.of(2).minus(worked),
                    regularRate,
                    regularRate.times(Hours.of(2).minus(worked)),
                    new Explanation(
                            "CA_REPORTING_TIME",
                            "IWC Wage Order 4 § 5 (reporting time)",
                            Map.of("worked", worked.toString()),
                            "short shift reporting-time make-up",
                            "Reporting-time pay when the employee is required to report but is given little work."
                    )
            ));
        }
        return lines;
    }
}
