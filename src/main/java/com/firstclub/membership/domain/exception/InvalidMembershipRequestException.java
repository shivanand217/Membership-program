package com.firstclub.membership.domain.exception;

/**
 * A semantically invalid request that passed bean validation but violates a business rule
 * (e.g. inactive plan/tier, downgrade to the same tier, upgrade to a lower tier). Maps to HTTP 422.
 */
public class InvalidMembershipRequestException extends MembershipException {

    public InvalidMembershipRequestException(String message) {
        super(message);
    }
}
