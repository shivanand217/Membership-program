package com.firstclub.membership.domain.enums;

/**
 * Lifecycle states of a subscription.
 *
 * <p>Only three terminal-or-active states are modelled. {@code PENDING} (awaiting payment) and
 * {@code GRACE_PERIOD} (past-due but recoverable) are deliberately left as future seams — subscribe
 * activates synchronously because payment is stubbed.
 */
public enum SubscriptionStatus {
    ACTIVE,
    CANCELLED,
    EXPIRED
}
