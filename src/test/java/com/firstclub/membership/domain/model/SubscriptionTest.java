package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.exception.IllegalStateTransitionException;
import com.firstclub.membership.domain.pricing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private final MembershipPlan plan =
            new MembershipPlan("MONTHLY", "Monthly", BillingCycle.MONTHLY, Money.inr(200));
    private final Tier silver = new Tier(TierLevel.SILVER, "Silver", 0, new BigDecimal("1.00"), "");
    private final Tier gold = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.50"), "");
    private final Tier platinum = new Tier(TierLevel.PLATINUM, "Platinum", 2, new BigDecimal("2.00"), "");

    private Subscription newSubscription(Tier selected, Tier earned) {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        return new Subscription(1L, plan, selected, earned, now, now.plusSeconds(86400),
                Money.inr(200), true, null);
    }

    @Test
    @DisplayName("ACTIVE may move to CANCELLED or EXPIRED")
    void activeMayCancelOrExpire() {
        Subscription s = newSubscription(silver, silver);
        s.transitionTo(SubscriptionStatus.CANCELLED);
        assertThat(s.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("terminal states cannot be revived")
    void terminalStatesAreFinal() {
        Subscription s = newSubscription(silver, silver);
        s.transitionTo(SubscriptionStatus.EXPIRED);
        assertThatThrownBy(() -> s.transitionTo(SubscriptionStatus.ACTIVE))
                .isInstanceOf(IllegalStateTransitionException.class);
    }

    @Test
    @DisplayName("effective tier is the higher-ranked of earned and selected")
    void effectiveTierIsMaxOfEarnedAndSelected() {
        assertThat(newSubscription(silver, gold).effectiveTier()).isEqualTo(gold);
        assertThat(newSubscription(gold, silver).effectiveTier()).isEqualTo(gold);
        assertThat(newSubscription(silver, silver).effectiveTier()).isEqualTo(silver);
    }

    @Test
    @DisplayName("earned and selected tiers are written independently (no thrash)")
    void earnedAndSelectedAreIndependent() {
        Subscription s = newSubscription(silver, silver);

        // system path writes earnedTier only
        s.assignEarnedTier(platinum);
        assertThat(s.getSelectedTier()).isEqualTo(silver);
        assertThat(s.effectiveTier()).isEqualTo(platinum);

        // user path writes selectedTier only
        s.changeSelectedTier(gold, Money.inr(300));
        assertThat(s.getEarnedTier()).isEqualTo(platinum);
        assertThat(s.getSelectedTier()).isEqualTo(gold);
        assertThat(s.effectiveTier()).isEqualTo(platinum); // earned still highest
    }

    @Test
    @DisplayName("an explicit tier change clears a pending downgrade")
    void upgradeClearsPendingDowngrade() {
        Subscription s = newSubscription(gold, gold);
        s.schedulePendingDowngrade(silver);
        assertThat(s.getPendingTier()).isEqualTo(silver);

        s.changeSelectedTier(platinum, Money.inr(400));
        assertThat(s.getPendingTier()).isNull();
    }
}
