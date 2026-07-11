package com.firstclub.membership.domain.enums;

import java.time.Period;

/**
 * Billing cadence of a {@code MembershipPlan}. The {@link Period} is the single source of truth for
 * how long a purchased period lasts — the subscription end date is always derived from it, never stored
 * as a magic number.
 */
public enum BillingCycle {

    MONTHLY(Period.ofMonths(1)),
    QUARTERLY(Period.ofMonths(3)),
    YEARLY(Period.ofYears(1));

    private final Period period;

    BillingCycle(Period period) {
        this.period = period;
    }

    public Period getPeriod() {
        return period;
    }
}
