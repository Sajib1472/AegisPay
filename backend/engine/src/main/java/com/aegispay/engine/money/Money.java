package com.aegispay.engine.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money uses scale 4 internally and scale 2 on statements.
 */
public final class Money {

    public static final Money ZERO = new Money(BigDecimal.ZERO);
    private static final int SCALE = 4;
    private static final RoundingMode MODE = RoundingMode.HALF_UP;

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount.setScale(SCALE, MODE);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(Hours hours) {
        return new Money(amount.multiply(hours.toBigDecimal()));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money half() {
        return new Money(amount.divide(BigDecimal.valueOf(2), SCALE, MODE));
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }

    public BigDecimal toStatement() {
        return amount.setScale(2, MODE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return toStatement().toPlainString();
    }
}
