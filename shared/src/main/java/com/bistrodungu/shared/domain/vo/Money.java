package com.bistrodungu.shared.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object for Money.
 * CRITICAL: Never use double for monetary calculations.
 * This class ensures correct precision and prevents rounding errors.
 */
public record Money(BigDecimal amount, String currency) implements Serializable {
    private static final String DEFAULT_CURRENCY = "COP";
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");

        if (amount.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative: " + amount);
        }

        // Normalize scale to 2 decimal places
        amount = amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money ofZero() {
        return new Money(ZERO, DEFAULT_CURRENCY);
    }

    public static Money ofZero(String currency) {
        return new Money(ZERO, currency);
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies: " +
                    this.currency + " and " + other.currency);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies: " +
                    this.currency + " and " + other.currency);
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Result would be negative: " + result);
        }
        return new Money(result, this.currency);
    }

    public Money multiply(int factor) {
        if (factor < 0) {
            throw new IllegalArgumentException("Multiplication factor cannot be negative: " + factor);
        }
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Multiplication factor cannot be negative: " + factor);
        }
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public Money divide(int divisor) {
        if (divisor <= 0) {
            throw new IllegalArgumentException("Division divisor must be positive: " + divisor);
        }
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor),
                2, java.math.RoundingMode.HALF_UP), this.currency);
    }

    public boolean isZero() {
        return this.amount.compareTo(ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) < 0;
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
