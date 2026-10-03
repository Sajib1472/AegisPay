package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FLSA weekly overtime: hours over 40 in the workweek at one-and-one-half the regular rate.
 * Daily California overtime is peeled first; remaining weekly surplus is FLSA OT.
 */
public final class FlsaOvertimeCalculator {

    public static final Hours WEEKLY_THRESHOLD = Hours.of(40);

    private FlsaOvertimeCalculator() {
    }

    public record WeekBuckets(Hours regular, Hours ot15, Hours doubleTime) {
        public Hours worked() {
            return regular.plus(ot15).plus(doubleTime);
        }
    }

    /**
     * After daily CA split, any remaining hours that push the week over 40 become FLSA OT
     * (unless already paid at 1.5x or 2x as daily OT).
     */
    public static WeekBuckets applyWeeklyOvertime(Hours alreadyRegular, Hours alreadyOt, Hours alreadyDt) {
        Hours worked = alreadyRegular.plus(alreadyOt).plus(alreadyDt);
        if (!worked.isGreaterThan(WEEKLY_THRESHOLD)) {
            return new WeekBuckets(alreadyRegular, alreadyOt, alreadyDt);
        }
        Hours weeklyOtNeeded = worked.minus(WEEKLY_THRESHOLD);
        Hours extraFromRegular = weeklyOtNeeded.min(alreadyRegular);
        Hours regular = alreadyRegular.minus(extraFromRegular);
        Hours ot15 = alreadyOt.plus(extraFromRegular);
        return new WeekBuckets(regular, ot15, alreadyDt);
    }

    public static List<EarningsLine> toLines(
            LocalDate weekEnding,
            Hours regularHours,
            Hours otHours,
            Hours doubleTimeHours,
            Money straightRate,
            Money regularRate
    ) {
        List<EarningsLine> lines = new ArrayList<>();
        if (!regularHours.isZero()) {
            lines.add(new EarningsLine(
                    weekEnding,
                    EarningBucket.REG,
                    regularHours,
                    straightRate,
                    straightRate.times(regularHours),
                    new Explanation(
                            "FLSA_REG",
                            "29 CFR 778.110",
                            Map.of("hours", regularHours.toString(), "rate", straightRate.toString()),
                            regularHours + " × $" + straightRate,
                            "Straight-time hours paid at the applicable hourly rate."
                    )
            ));
        }
        if (!otHours.isZero()) {
            Money otRate = regularRate.times(java.math.BigDecimal.valueOf(1.5));
            lines.add(new EarningsLine(
                    weekEnding,
                    EarningBucket.OT_1_5,
                    otHours,
                    otRate,
                    otRate.times(otHours),
                    new Explanation(
                            "FLSA_OT_1_5",
                            "29 U.S.C. § 207(a); 29 CFR 778.110",
                            Map.of(
                                    "hours", otHours.toString(),
                                    "regularRate", regularRate.toString(),
                                    "threshold", "40"
                            ),
                            otHours + " × 1.5 × $" + regularRate,
                            "Weekly overtime at one-and-one-half the regular rate for hours over 40."
                    )
            ));
        }
        if (!doubleTimeHours.isZero()) {
            Money dtRate = regularRate.times(java.math.BigDecimal.valueOf(2));
            lines.add(new EarningsLine(
                    weekEnding,
                    EarningBucket.OT_2_0,
                    doubleTimeHours,
                    dtRate,
                    dtRate.times(doubleTimeHours),
                    new Explanation(
                            "CA_DT_2_0",
                            "IWC Wage Order daily double-time",
                            Map.of("hours", doubleTimeHours.toString(), "regularRate", regularRate.toString()),
                            doubleTimeHours + " × 2.0 × $" + regularRate,
                            "Double-time hours (California daily rule) at twice the regular rate."
                    )
            ));
        }
        return lines;
    }
}
