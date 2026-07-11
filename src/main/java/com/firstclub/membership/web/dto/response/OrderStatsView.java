package com.firstclub.membership.web.dto.response;

import java.time.Instant;

/** A user's ordering activity, as used for tier progression. */
public record OrderStatsView(Long userId,
                             long lifetimeOrderCount,
                             long ordersThisMonth,
                             MoneyDto spendThisMonth,
                             Instant lastOrderAt) {
}
