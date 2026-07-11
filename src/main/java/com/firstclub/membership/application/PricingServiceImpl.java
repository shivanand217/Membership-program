package com.firstclub.membership.application;

import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.pricing.PricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

/**
 * Default pricing: base plan price scaled by the tier multiplier, with time-based proration for upgrades.
 * Pure and deterministic — no persistence — so it is trivially unit-testable.
 */
@Service
public class PricingServiceImpl implements PricingService {

    private static final int FRACTION_SCALE = 6;

    @Override
    public Money quote(MembershipPlan plan, Tier tier) {
        return plan.getPrice().multiply(tier.getPriceMultiplier());
    }

    @Override
    public Money proratedUpgradeDelta(Subscription current, MembershipPlan plan, Tier newTier, Instant now) {
        Money newPeriodPrice = quote(plan, newTier);
        Money currentPeriodPrice = current.getPricePaid();
        BigDecimal remainingFraction = remainingFraction(current.getStartsAt(), current.getEndsAt(), now);
        // (new - old) * remainingFraction, never negative.
        return newPeriodPrice.subtract(currentPeriodPrice).multiply(remainingFraction).atLeastZero();
    }

    /** Fraction of the current period still unused, clamped to [0, 1]. */
    private BigDecimal remainingFraction(Instant start, Instant end, Instant now) {
        long totalSeconds = Duration.between(start, end).getSeconds();
        if (totalSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        long remainingSeconds = Duration.between(now, end).getSeconds();
        if (remainingSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        if (remainingSeconds >= totalSeconds) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(remainingSeconds)
                .divide(BigDecimal.valueOf(totalSeconds), FRACTION_SCALE, RoundingMode.HALF_UP);
    }
}
