package com.firstclub.membership.domain.pricing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("arithmetic keeps a 2-dp scale and the currency")
    void arithmeticKeepsScaleAndCurrency() {
        Money result = Money.inr(100).add(Money.inr(50)).subtract(Money.inr(25));
        assertThat(result.getAmount()).isEqualByComparingTo("125.00");
        assertThat(result.getCurrency()).isEqualTo("INR");
    }

    @Test
    @DisplayName("multiply applies a scalar factor and rounds to 2dp")
    void multiplyScales() {
        assertThat(Money.inr(200).multiply(new BigDecimal("1.50")).getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("mixing currencies is rejected")
    void currencyMismatchThrows() {
        assertThatThrownBy(() -> Money.inr(10).add(Money.of(BigDecimal.TEN, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
    }

    @Test
    @DisplayName("atLeastZero floors a negative amount at zero")
    void atLeastZeroFloorsNegative() {
        assertThat(Money.inr(10).subtract(Money.inr(30)).atLeastZero().getAmount()).isEqualByComparingTo("0.00");
        assertThat(Money.inr(30).subtract(Money.inr(10)).atLeastZero().getAmount()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("equality is by value, ignoring trailing zeros")
    void equalsIgnoresTrailingZeros() {
        assertThat(Money.of(new BigDecimal("100.0"), "INR")).isEqualTo(Money.of(new BigDecimal("100.00"), "INR"));
    }
}
