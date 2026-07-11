package com.firstclub.membership.domain.eligibility;

import com.firstclub.membership.domain.pricing.Money;

import java.time.Instant;
import java.util.Set;

/**
 * Immutable snapshot of everything the eligibility rules need to judge a user, gathered once per
 * evaluation. Passing a pre-computed context (rather than repositories) keeps every rule a pure,
 * side-effect-free function — trivial to unit test and safe to run concurrently.
 *
 * @param userId             the user under evaluation
 * @param lifetimeOrderCount total orders ever placed
 * @param ordersThisMonth    orders in the current business month
 * @param spendThisMonth     order value in the current business month
 * @param cohorts            cohort codes the user actively belongs to
 * @param asOf               evaluation instant
 */
public record TierEvaluationContext(Long userId,
                                    long lifetimeOrderCount,
                                    long ordersThisMonth,
                                    Money spendThisMonth,
                                    Set<String> cohorts,
                                    Instant asOf) {
}
