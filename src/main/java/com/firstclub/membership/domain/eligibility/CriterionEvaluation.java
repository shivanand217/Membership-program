package com.firstclub.membership.domain.eligibility;

import com.firstclub.membership.domain.enums.CriteriaType;

/**
 * The outcome of checking one criterion against a user, with a human-readable requirement string — used
 * to explain <em>why</em> a user does or does not qualify for a tier.
 */
public record CriterionEvaluation(CriteriaType type, String requirement, boolean satisfied) {
}
