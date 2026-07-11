package com.firstclub.membership.domain.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable monetary value object: an amount plus an ISO currency code.
 *
 * <p>Modelled as a JPA {@code @Embeddable} so entities store {@code (amount, currency)} inline while the
 * whole codebase manipulates money through a single, safe type — never a raw {@link BigDecimal}. All
 * arithmetic keeps a scale of 2 (HALF_UP) and refuses to mix currencies.
 */
@Embeddable
public final class Money implements Comparable<Money> {

    public static final String DEFAULT_CURRENCY = "INR";
    public static final Money ZERO_INR = Money.inr(BigDecimal.ZERO);

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** For JPA only. */
    protected Money() {
    }

    private Money(BigDecimal amount, String currency) {
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Money of(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        return new Money(amount, currency);
    }

    public static Money inr(BigDecimal amount) {
        return of(amount, DEFAULT_CURRENCY);
    }

    public static Money inr(long amount) {
        return of(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    public static Money zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return of(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return of(amount.subtract(other.amount), currency);
    }

    /** Multiply by a scalar (e.g. a tier price multiplier or a remaining-period fraction). */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        return of(amount.multiply(factor), currency);
    }

    public Money max(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) >= 0 ? this : other;
    }

    public Money min(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) <= 0 ? this : other;
    }

    /** Floors a possibly-negative amount at zero (never returns a debt). */
    public Money atLeastZero() {
        return isNegative() ? zero(currency) : this;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) >= 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money o) {
        requireSameCurrency(o);
        return amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        // Value equality: 100.00 == 100.0, and currency must match.
        return currency.equals(money.currency) && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount.stripTrailingZeros());
    }

    @Override
    public String toString() {
        return currency + " " + amount.toPlainString();
    }
}
