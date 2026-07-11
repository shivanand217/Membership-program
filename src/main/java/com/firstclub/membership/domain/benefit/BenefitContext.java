package com.firstclub.membership.domain.benefit;

import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.vo.OrderPreview;

import java.time.Instant;

/**
 * Input to a {@link BenefitHandler}. The {@code order} is present when evaluating a concrete basket
 * (the preview endpoint) and null when merely listing what a tier offers.
 *
 * @param userId        member the benefit applies to
 * @param effectiveTier the tier whose configured perks are being resolved
 * @param order         the order under evaluation, or {@code null} for a listing-only context
 * @param evaluatedAt   evaluation instant
 */
public record BenefitContext(Long userId, Tier effectiveTier, OrderPreview order, Instant evaluatedAt) {

    public boolean hasOrder() {
        return order != null;
    }
}
