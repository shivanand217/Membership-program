package com.firstclub.membership.application;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.pricing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceImplTest {

    private final PricingServiceImpl pricing = new PricingServiceImpl();

    private final MembershipPlan monthly =
            new MembershipPlan("MONTHLY", "Monthly", BillingCycle.MONTHLY, Money.inr(200));
    private final Tier silver = new Tier(TierLevel.SILVER, "Silver", 0, new BigDecimal("1.00"), "");
    private final Tier gold = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.50"), "");

    @Test
    @DisplayName("quote = base price x tier multiplier")
    void quoteMultipliesByTierMultiplier() {
        assertThat(pricing.quote(monthly, silver).getAmount()).isEqualByComparingTo("200.00");
        assertThat(pricing.quote(monthly, gold).getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("prorated upgrade charges the price difference for the remaining half-period")
    void proratedUpgradeChargesForRemainingPeriod() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-31T00:00:00Z");   // 30-day period
        Instant now = Instant.parse("2026-01-16T00:00:00Z");   // 15 days remaining -> fraction 0.5
        Subscription sub = new Subscription(1L, monthly, silver, silver, start, end, Money.inr(200), true, null);

        Money delta = pricing.proratedUpgradeDelta(sub, monthly, gold, now);

        // (300 - 200) * 0.5 = 50.00
        assertThat(delta.getAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("prorated delta is never negative in the downgrade direction")
    void proratedDeltaFlooredAtZero() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Instant end = Instant.parse("2026-01-31T00:00:00Z");
        Instant now = Instant.parse("2026-01-16T00:00:00Z");
        Subscription goldSub = new Subscription(1L, monthly, gold, gold, start, end, Money.inr(300), true, null);

        Money delta = pricing.proratedUpgradeDelta(goldSub, monthly, silver, now);

        assertThat(delta.getAmount()).isEqualByComparingTo("0.00");
    }
}
