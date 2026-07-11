package com.firstclub.membership.domain.exception;

/** A referenced entity (user, plan, tier, subscription) does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends MembershipException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
