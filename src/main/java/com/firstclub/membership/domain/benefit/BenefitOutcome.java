package com.firstclub.membership.domain.benefit;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.pricing.Money;

import java.util.Map;

/**
 * The result of applying one benefit. {@code monetaryEffect} is the money saved (a discount amount or a
 * waived delivery fee); it is {@link Money#ZERO_INR zero} for informational perks. {@code attributes}
 * carries perk-specific detail for the API response (percentage used, SLA minutes, coupon count...).
 */
public record BenefitOutcome(BenefitType type,
                             boolean applied,
                             Money monetaryEffect,
                             Map<String, Object> attributes,
                             String description) {

    public static BenefitOutcome notApplied(BenefitType type, String reason) {
        return new BenefitOutcome(type, false, Money.ZERO_INR, Map.of(), reason);
    }

    public static BenefitOutcome monetary(BenefitType type, Money effect, Map<String, Object> attributes, String description) {
        return new BenefitOutcome(type, true, effect, attributes == null ? Map.of() : attributes, description);
    }

    /** A perk that applies but moves no money (exclusive deals, priority support, coupons...). */
    public static BenefitOutcome informational(BenefitType type, Map<String, Object> attributes, String description) {
        return new BenefitOutcome(type, true, Money.ZERO_INR, attributes == null ? Map.of() : attributes, description);
    }
}
