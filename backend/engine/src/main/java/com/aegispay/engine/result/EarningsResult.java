package com.aegispay.engine.result;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record EarningsResult(
        String employeeId,
        String engineVersion,
        List<String> rulePackVersions,
        Money regularRate,
        List<EarningsLine> lines,
        List<PayException> exceptions,
        List<String> warnings,
        Totals totals
) {
    public EarningsResult {
        rulePackVersions = List.copyOf(rulePackVersions);
        lines = List.copyOf(lines);
        exceptions = List.copyOf(exceptions);
        warnings = List.copyOf(warnings);
    }

    public enum EarningBucket {
        REG,
        OT_1_5,
        OT_2_0,
        MEAL_PREMIUM,
        REST_PREMIUM,
        SPLIT_SHIFT,
        REPORTING_TIME,
        DIFFERENTIAL,
        BONUS,
        BONUS_TRUE_UP
    }

    public record Explanation(
            String code,
            String citation,
            Map<String, String> inputs,
            String formula,
            String narrative
    ) {
    }

    public record EarningsLine(
            LocalDate workDate,
            EarningBucket bucket,
            Hours hours,
            Money rate,
            Money amount,
            Explanation explanation
    ) {
    }

    public record PayException(
            String type,
            String severity,
            boolean blocker,
            LocalDate workDate,
            String message
    ) {
    }

    public record Totals(
            Hours regularHours,
            Hours otHours,
            Hours doubleTimeHours,
            Money regularPay,
            Money overtimePay,
            Money premiumPay,
            Money bonusPay,
            Money gross
    ) {
        public static Totals from(List<EarningsLine> lines) {
            Hours reg = Hours.ZERO;
            Hours ot = Hours.ZERO;
            Hours dt = Hours.ZERO;
            Money regularPay = Money.ZERO;
            Money overtimePay = Money.ZERO;
            Money premiumPay = Money.ZERO;
            Money bonusPay = Money.ZERO;
            for (EarningsLine line : lines) {
                switch (line.bucket()) {
                    case REG -> {
                        reg = reg.plus(line.hours());
                        regularPay = regularPay.plus(line.amount());
                    }
                    case OT_1_5, DIFFERENTIAL -> {
                        ot = ot.plus(line.hours());
                        overtimePay = overtimePay.plus(line.amount());
                    }
                    case OT_2_0 -> {
                        dt = dt.plus(line.hours());
                        overtimePay = overtimePay.plus(line.amount());
                    }
                    case MEAL_PREMIUM, REST_PREMIUM, SPLIT_SHIFT, REPORTING_TIME ->
                            premiumPay = premiumPay.plus(line.amount());
                    case BONUS, BONUS_TRUE_UP -> bonusPay = bonusPay.plus(line.amount());
                }
            }
            Money gross = regularPay.plus(overtimePay).plus(premiumPay).plus(bonusPay);
            return new Totals(reg, ot, dt, regularPay, overtimePay, premiumPay, bonusPay, gross);
        }
    }

    public static Builder builder(String employeeId, String engineVersion) {
        return new Builder(employeeId, engineVersion);
    }

    public static final class Builder {
        private final String employeeId;
        private final String engineVersion;
        private final List<String> rulePackVersions = new ArrayList<>();
        private final List<EarningsLine> lines = new ArrayList<>();
        private final List<PayException> exceptions = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private Money regularRate = Money.ZERO;

        private Builder(String employeeId, String engineVersion) {
            this.employeeId = employeeId;
            this.engineVersion = engineVersion;
        }

        public Builder rulePack(String version) {
            rulePackVersions.add(version);
            return this;
        }

        public Builder regularRate(Money rate) {
            this.regularRate = rate;
            return this;
        }

        public Builder line(EarningsLine line) {
            lines.add(line);
            return this;
        }

        public Builder exception(PayException exception) {
            exceptions.add(exception);
            return this;
        }

        public Builder warning(String warning) {
            warnings.add(warning);
            return this;
        }

        public EarningsResult build() {
            return new EarningsResult(
                    employeeId,
                    engineVersion,
                    rulePackVersions,
                    regularRate,
                    lines,
                    exceptions,
                    warnings,
                    Totals.from(lines)
            );
        }
    }
}
