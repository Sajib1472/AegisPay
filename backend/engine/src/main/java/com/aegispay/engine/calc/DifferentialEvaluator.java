package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod.PayRate;
import com.aegispay.engine.model.WorkPeriod.RateType;
import com.aegispay.engine.model.WorkPeriod.Shift;
import com.aegispay.engine.result.EarningsResult.EarningBucket;
import com.aegispay.engine.result.EarningsResult.EarningsLine;
import com.aegispay.engine.result.EarningsResult.Explanation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DifferentialEvaluator {

    private DifferentialEvaluator() {
    }

    public static List<EarningsLine> apply(Shift shift, Hours hours, LocalDate date) {
        List<EarningsLine> lines = new ArrayList<>();
        for (PayRate rate : shift.rates()) {
            if (rate.type() == RateType.DIFFERENTIAL_FLAT) {
                Money amount = rate.hourly().times(hours);
                lines.add(new EarningsLine(
                        date,
                        EarningBucket.DIFFERENTIAL,
                        hours,
                        rate.hourly(),
                        amount,
                        new Explanation(
                                "DIFF_FLAT",
                                "Tenant differential policy (included in regular rate if non-discretionary)",
                                Map.of("hours", hours.toString(), "diff", rate.hourly().toString()),
                                hours + " × $" + rate.hourly(),
                                "Flat shift differential."
                        )
                ));
            } else if (rate.type() == RateType.DIFFERENTIAL_PERCENT) {
                Money base = shift.rates().stream()
                        .filter(r -> r.type() == RateType.HOURLY)
                        .map(PayRate::hourly)
                        .findFirst()
                        .orElse(Money.ZERO);
                Money diffRate = Money.of(base.toBigDecimal().multiply(rate.hourly().toBigDecimal()));
                lines.add(new EarningsLine(
                        date,
                        EarningBucket.DIFFERENTIAL,
                        hours,
                        diffRate,
                        diffRate.times(hours),
                        new Explanation(
                                "DIFF_PCT",
                                "Tenant differential policy",
                                Map.of("percent", rate.hourly().toString()),
                                hours + " × base × " + rate.hourly(),
                                "Percentage shift differential. Enters the regular rate."
                        )
                ));
            }
        }
        return lines;
    }
}
