package com.firstclub.membership.domain.pricing;

import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.Tier;

import java.time.Instant;

/**
 * Pricing rules for the program. Kept behind an interface (implementation lives in the application layer)
 * so callers depend on the abstraction, and an alternative scheme (promotional, region-based) can be
 * swapped in without touching the subscription service (Dependency Inversion).
 */
public interface PricingService {

    /** Price of one period of {@code plan} at {@code tier} = base price × tier multiplier. */
    Money quote(MembershipPlan plan, Tier tier);

    /**
     * Prorated amount to charge for an immediate upgrade to {@code newTier}: the price difference for the
     * fraction of the current period that remains, floored at zero.
     */
    Money proratedUpgradeDelta(Subscription current, MembershipPlan plan, Tier newTier, Instant now);
}
