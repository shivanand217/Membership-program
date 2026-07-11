package com.firstclub.membership.domain.enums;

/**
 * The kinds of progression criteria that gate <em>free auto-promotion</em> into a tier. Each value is
 * backed by exactly one {@code TierEligibilityRule} strategy. Adding a new criterion = add a value here
 * + one rule class (Open/Closed).
 */
public enum CriteriaType {
    /** Lifetime order count at or above a threshold. */
    MIN_LIFETIME_ORDERS,
    /** Orders placed in the current business month at or above a threshold. */
    MIN_MONTHLY_ORDERS,
    /** Order value accumulated in the current business month at or above a threshold. */
    MIN_MONTHLY_ORDER_VALUE,
    /** Membership of a named cohort. */
    COHORT_MEMBERSHIP
}
