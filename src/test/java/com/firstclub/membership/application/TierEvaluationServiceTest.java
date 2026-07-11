package com.firstclub.membership.application;

import com.firstclub.membership.domain.eligibility.TierEligibilityRule;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.eligibility.rule.CohortMembershipRule;
import com.firstclub.membership.domain.eligibility.rule.MinLifetimeOrdersRule;
import com.firstclub.membership.domain.eligibility.rule.MinMonthlyOrderValueRule;
import com.firstclub.membership.domain.eligibility.rule.MinMonthlyOrdersRule;
import com.firstclub.membership.domain.enums.CriteriaMatch;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.pricing.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TierEvaluationServiceTest {

    private final List<TierEligibilityRule> rules = List.of(
            new MinLifetimeOrdersRule(), new MinMonthlyOrdersRule(),
            new MinMonthlyOrderValueRule(), new CohortMembershipRule());
    private final TierEvaluationService service = new TierEvaluationService(null, rules);

    private final Tier silver = new Tier(TierLevel.SILVER, "Silver", 0, new BigDecimal("1.00"), "");
    private final Tier gold = goldTier();
    private final Tier platinum = platinumTier();

    // Highest rank first, as the repository would return.
    private final List<Tier> tiersHighToLow = List.of(platinum, gold, silver);

    private Tier goldTier() {
        Tier t = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.50"), "")
                .setCriteriaMatch(CriteriaMatch.ANY);
        t.addCriterion(TierCriterion.numeric(CriteriaType.MIN_LIFETIME_ORDERS, new BigDecimal("5")));
        t.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDER_VALUE, new BigDecimal("2000")));
        t.addCriterion(TierCriterion.cohort("early_adopter"));
        return t;
    }

    private Tier platinumTier() {
        Tier t = new Tier(TierLevel.PLATINUM, "Platinum", 2, new BigDecimal("2.00"), "")
                .setCriteriaMatch(CriteriaMatch.ANY);
        t.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDERS, new BigDecimal("10")));
        t.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDER_VALUE, new BigDecimal("10000")));
        t.addCriterion(TierCriterion.cohort("prime_metro"));
        return t;
    }

    private TierEvaluationContext ctx(long lifetime, long monthlyOrders, long monthlyValue, Set<String> cohorts) {
        return new TierEvaluationContext(1L, lifetime, monthlyOrders, Money.inr(monthlyValue), cohorts, Instant.now());
    }

    @Test
    @DisplayName("no activity earns the base tier")
    void noActivityEarnsBase() {
        assertThat(service.selectEarnedTier(tiersHighToLow, ctx(0, 0, 0, Set.of()))).isEqualTo(silver);
    }

    @Test
    @DisplayName("crossing a single Gold path (lifetime orders) earns Gold")
    void lifetimeOrdersEarnGold() {
        assertThat(service.selectEarnedTier(tiersHighToLow, ctx(5, 0, 0, Set.of()))).isEqualTo(gold);
    }

    @Test
    @DisplayName("cohort membership alone can earn Platinum (ANY match)")
    void cohortAloneEarnsPlatinum() {
        assertThat(service.selectEarnedTier(tiersHighToLow, ctx(0, 0, 0, Set.of("prime_metro")))).isEqualTo(platinum);
    }

    @Test
    @DisplayName("monthly volume earns Platinum and beats a lower qualifying tier")
    void monthlyVolumeEarnsPlatinum() {
        assertThat(service.selectEarnedTier(tiersHighToLow, ctx(5, 12, 12000, Set.of()))).isEqualTo(platinum);
    }

    @Test
    @DisplayName("ALL match requires every criterion")
    void allMatchRequiresEveryCriterion() {
        Tier strict = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.5"), "")
                .setCriteriaMatch(CriteriaMatch.ALL);
        strict.addCriterion(TierCriterion.numeric(CriteriaType.MIN_LIFETIME_ORDERS, new BigDecimal("5")));
        strict.addCriterion(TierCriterion.numeric(CriteriaType.MIN_MONTHLY_ORDER_VALUE, new BigDecimal("2000")));

        assertThat(service.qualifies(strict, ctx(5, 0, 0, Set.of()))).isFalse();       // value missing
        assertThat(service.qualifies(strict, ctx(5, 0, 2000, Set.of()))).isTrue();     // both satisfied
    }
}
