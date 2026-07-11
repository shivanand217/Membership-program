package com.firstclub.membership.domain.exception;

import com.firstclub.membership.domain.enums.SubscriptionStatus;

/**
 * Thrown when a subscription is asked to move to a status the transition table forbids
 * (e.g. reviving a CANCELLED subscription).
 */
public class IllegalStateTransitionException extends MembershipException {

    public IllegalStateTransitionException(SubscriptionStatus from, SubscriptionStatus to) {
        super("Illegal subscription transition: " + from + " -> " + to);
    }
}
