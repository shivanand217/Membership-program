package com.firstclub.membership.application;

import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.pricing.Money;

import java.util.List;

/**
 * Result of applying a tier's configured benefits to a sample order: the per-benefit outcomes plus the
 * rolled-up money figures after the stacking policy (additive percentage discounts under one global cap,
 * plus independent free delivery).
 */
public record BenefitPreviewResult(TierLevel tier,
                                   List<BenefitOutcome> outcomes,
                                   Money subtotal,
                                   Money deliveryFee,
                                   Money discountOnSubtotal,
                                   Money deliveryWaived,
                                   Money totalDiscount,
                                   Money finalPayable,
                                   boolean deliveryFreed) {
}
