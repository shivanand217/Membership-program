package com.firstclub.membership.domain.exception;

/**
 * Base type for all domain-level failures. Having a common root lets the web layer translate the whole
 * family into RFC-7807 {@code ProblemDetail} responses in one place.
 */
public abstract class MembershipException extends RuntimeException {

    protected MembershipException(String message) {
        super(message);
    }

    protected MembershipException(String message, Throwable cause) {
        super(message, cause);
    }
}
