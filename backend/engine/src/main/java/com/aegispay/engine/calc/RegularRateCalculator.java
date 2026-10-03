package com.aegispay.engine.calc;

import com.aegispay.engine.money.Hours;
import com.aegispay.engine.money.Money;
import com.aegispay.engine.model.WorkPeriod;
import com.aegispay.engine.model.WorkPeriod.Bonus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Regular rate of pay: (straight-time earnings + non-discretionary bonuses) / hours worked.
 * See 29 CFR 778.110 and 778.209.
 */
public final class RegularRateCalculator {

    private RegularRateCalculator() {
    }

    public static Money calculate(Hours hoursWorked, Money straightTimeEarnings, List<Bonus> bonuses) {
        if (hoursWorked.isZero()) {
            return Money.ZERO;
        }
        Money includedBonuses = Money.ZERO;
        for (Bonus bonus : bonuses) {
            if (!bonus.discretionary()) {
                includedBonuses = includedBonuses.plus(bonus.amount());
            }
        }
        Money numerator = straightTimeEarnings.plus(includedBonuses);
        BigDecimal rate = numerator.toBigDecimal()
                .divide(hoursWorked.toBigDecimal(), 4, RoundingMode.HALF_UP);
        return Money.of(rate);
    }

    public static Money nonDiscretionaryBonusTotal(WorkPeriod period) {
        Money total = Money.ZERO;
        for (Bonus bonus : period.bonuses()) {
            if (!bonus.discretionary()) {
                total = total.plus(bonus.amount());
            }
        }
        return total;
    }
}
