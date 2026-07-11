package com.firstclub.membership.web.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Result of applying a member's benefits to a sample order: each perk's outcome plus the rolled-up money
 * figures (discount on subtotal after the global cap, waived delivery, total saved, final payable).
 */
public record BenefitPreviewResponse(String tier,
                                     List<BenefitOutcomeView> outcomes,
                                     MoneyDto subtotal,
                                     MoneyDto deliveryFee,
                                     MoneyDto discountOnSubtotal,
                                     MoneyDto deliveryWaived,
                                     MoneyDto totalDiscount,
                                     MoneyDto finalPayable,
                                     boolean deliveryFreed) {

    public record BenefitOutcomeView(String benefitType,
                                     boolean applied,
                                     MoneyDto monetaryEffect,
                                     String description,
                                     Map<String, Object> attributes) {
    }
}
