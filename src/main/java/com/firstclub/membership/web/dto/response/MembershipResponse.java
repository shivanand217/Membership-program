package com.firstclub.membership.web.dto.response;

import java.time.Instant;

/**
 * Lightweight "current membership" view: what a user has right now, including the effective tier and the
 * expiry. Used by the track-membership endpoint.
 */
public record MembershipResponse(Long userId,
                                 Long subscriptionId,
                                 String status,
                                 String planCode,
                                 String billingCycle,
                                 String effectiveTier,
                                 String earnedTier,
                                 String selectedTier,
                                 String pendingTier,
                                 MoneyDto pricePaid,
                                 Instant startsAt,
                                 Instant endsAt,
                                 boolean autoRenew,
                                 boolean cancelAtPeriodEnd) {
}
