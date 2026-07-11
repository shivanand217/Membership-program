package com.firstclub.membership.domain.exception;

import com.firstclub.membership.domain.enums.BenefitType;

/** No handler is registered for a configured benefit type — a configuration/wiring error. */
public class UnknownBenefitTypeException extends MembershipException {

    public UnknownBenefitTypeException(BenefitType type) {
        super("No BenefitHandler registered for type: " + type);
    }
}
