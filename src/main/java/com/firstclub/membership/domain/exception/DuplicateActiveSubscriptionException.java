package com.firstclub.membership.domain.exception;

/** A user already has an ACTIVE subscription. Maps to HTTP 409 (conflict). */
public class DuplicateActiveSubscriptionException extends MembershipException {

    public DuplicateActiveSubscriptionException(Long userId) {
        super("User " + userId + " already has an active subscription");
    }
}
