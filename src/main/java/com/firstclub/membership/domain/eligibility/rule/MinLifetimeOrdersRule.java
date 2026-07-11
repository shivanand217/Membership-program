package com.firstclub.membership.domain.eligibility.rule;

import com.firstclub.membership.domain.eligibility.TierEligibilityRule;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.model.TierCriterion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Satisfied when the user's lifetime order count is at or above the threshold. */
@Component
public class MinLifetimeOrdersRule implements TierEligibilityRule {

    @Override
    public CriteriaType type() {
        return CriteriaType.MIN_LIFETIME_ORDERS;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, TierEvaluationContext ctx) {
        return BigDecimal.valueOf(ctx.lifetimeOrderCount()).compareTo(criterion.getThreshold()) >= 0;
    }
}
