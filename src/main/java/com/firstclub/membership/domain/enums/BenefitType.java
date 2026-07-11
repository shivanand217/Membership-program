package com.firstclub.membership.domain.enums;

/**
 * The kinds of benefit the platform knows how to reason about. Each value is backed by exactly one
 * {@code BenefitHandler} strategy. Adding a new perk category = add a value here + one handler class;
 * no existing code changes (Open/Closed).
 */
public enum BenefitType {
    /** Waive the delivery fee when the order qualifies (e.g. above a minimum value). */
    FREE_DELIVERY,
    /** Extra percentage discount, optionally scoped to categories and capped. */
    PERCENTAGE_DISCOUNT,
    /** Access to members-only deals (informational / listing). */
    EXCLUSIVE_DEALS,
    /** Early access to sales windows (informational / listing). */
    EARLY_ACCESS_SALES,
    /** Priority customer support with an SLA (informational / listing). */
    PRIORITY_SUPPORT,
    /** A set of exclusive coupons granted to the member (informational / listing). */
    EXCLUSIVE_COUPONS
}
