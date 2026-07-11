package com.firstclub.membership.domain.eligibility;

import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.model.TierCriterion;

/**
 * Strategy for evaluating a single {@link CriteriaType}. One implementation per criterion type; Spring
 * auto-discovers them and the evaluator dispatches by {@link #type()}. Adding a new kind of progression
 * criterion is therefore a matter of adding an enum value and one bean — no existing rule changes
 * (Open/Closed, Single-Responsibility).
 */
public interface TierEligibilityRule {

    /** The criterion type this rule handles (registry key). */
    CriteriaType type();

    /** True iff {@code criterion} is satisfied by the user described in {@code ctx}. */
    boolean isSatisfied(TierCriterion criterion, TierEvaluationContext ctx);
}
