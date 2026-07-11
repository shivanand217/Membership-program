package com.firstclub.membership.domain.eligibility.rule;

import com.firstclub.membership.domain.eligibility.TierEligibilityRule;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.model.TierCriterion;
import org.springframework.stereotype.Component;

/** Satisfied when the user actively belongs to the criterion's cohort. */
@Component
public class CohortMembershipRule implements TierEligibilityRule {

    @Override
    public CriteriaType type() {
        return CriteriaType.COHORT_MEMBERSHIP;
    }

    @Override
    public boolean isSatisfied(TierCriterion criterion, TierEvaluationContext ctx) {
        return ctx.cohorts().contains(criterion.getCohortCode());
    }
}
