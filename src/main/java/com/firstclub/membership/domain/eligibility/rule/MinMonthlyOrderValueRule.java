package com.firstclub.membership.domain.eligibility.rule;

import com.firstclub.membership.domain.eligibility.TierEligibilityRule;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.model.TierCriterion;
import org.springframework.stereotype.Component;

/** Satisfied when order value in the current business month is at or above the threshold. */
@Component
public class MinMonthlyOrderValueRule implements TierEligibilityRule {

    @Override
    public CriteriaType type() {
        return CriteriaType.MIN_MONTHLY_ORDER_VALUE;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, TierEvaluationContext ctx) {
        return ctx.spendThisMonth().getAmount().compareTo(criterion.getThreshold()) >= 0;
    }
}
