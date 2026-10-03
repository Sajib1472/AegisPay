package com.aegispay.engine.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Objects;

/**
 * Hours freeze: NUMERIC(8,4). 0.25 = 15 minutes. HALF_UP.
 */
public final class Hours {

    public static final Hours ZERO = new Hours(BigDecimal.ZERO);
    private static final int SCALE = 4;
    private static final RoundingMode MODE = RoundingMode.HALF_UP;

    private final BigDecimal value;

    private Hours(BigDecimal value) {
        this.value = value.setScale(SCALE, MODE);
    }

    public static Hours of(BigDecimal value) {
        return new Hours(value);
    }

    public static Hours of(String value) {
        return new Hours(new BigDecimal(value));
    }

    public static Hours of(double value) {
        return new Hours(BigDecimal.valueOf(value));
    }

    public static Hours fromDuration(Duration duration) {
        BigDecimal seconds = BigDecimal.valueOf(duration.toSeconds());
        return new Hours(seconds.divide(BigDecimal.valueOf(3600), SCALE, MODE));
    }

    public Hours plus(Hours other) {
        return new Hours(value.add(other.value));
    }

    public Hours minus(Hours other) {
        return new Hours(value.subtract(other.value));
    }

    public Hours min(Hours other) {
        return value.compareTo(other.value) <= 0 ? this : other;
    }

    public Hours max(Hours other) {
        return value.compareTo(other.value) >= 0 ? this : other;
    }

    public boolean isGreaterThan(Hours other) {
        return value.compareTo(other.value) > 0;
    }

    public boolean isNegative() {
        return value.signum() < 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public BigDecimal toBigDecimal() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Hours hours)) {
            return false;
        }
        return value.compareTo(hours.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
