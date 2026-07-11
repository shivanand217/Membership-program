package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.pricing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class UserOrderStatsTest {

    private static final YearMonth JAN = YearMonth.of(2026, 1);
    private static final YearMonth FEB = YearMonth.of(2026, 2);

    @Test
    @DisplayName("recording orders accrues lifetime and monthly counters")
    void recordingAccruesCounters() {
        UserOrderStats stats = new UserOrderStats(1L, JAN, "INR");
        stats.recordOrder(Money.inr(450), Instant.now(), JAN);
        stats.recordOrder(Money.inr(550), Instant.now(), JAN);

        assertThat(stats.getLifetimeOrderCount()).isEqualTo(2);
        assertThat(stats.ordersThisMonth(JAN)).isEqualTo(2);
        assertThat(stats.valueThisMonth(JAN, "INR").getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("monthly counters read as zero in a later month (reset-on-read)")
    void monthlyCountersResetOnRead() {
        UserOrderStats stats = new UserOrderStats(1L, JAN, "INR");
        stats.recordOrder(Money.inr(1000), Instant.now(), JAN);

        // Same lifetime, but a new month sees zero monthly figures without any background job.
        assertThat(stats.ordersThisMonth(FEB)).isZero();
        assertThat(stats.valueThisMonth(FEB, "INR").getAmount()).isEqualByComparingTo("0.00");
        assertThat(stats.getLifetimeOrderCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("recording in a new month rolls the window and keeps lifetime")
    void recordingRollsMonthWindow() {
        UserOrderStats stats = new UserOrderStats(1L, JAN, "INR");
        stats.recordOrder(Money.inr(1000), Instant.now(), JAN);
        stats.recordOrder(Money.inr(700), Instant.now(), FEB);

        assertThat(stats.getLifetimeOrderCount()).isEqualTo(2);
        assertThat(stats.ordersThisMonth(FEB)).isEqualTo(1);
        assertThat(stats.valueThisMonth(FEB, "INR").getAmount()).isEqualByComparingTo("700.00");
    }
}
