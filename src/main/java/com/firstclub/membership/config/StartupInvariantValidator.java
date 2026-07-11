package com.firstclub.membership.config;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.TierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Fails startup fast if the catalog is misconfigured, so the application never serves traffic in an
 * inconsistent state. Runs after the seeder (higher {@link Order} value = later).
 */
@Component
@Order(20)
public class StartupInvariantValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInvariantValidator.class);

    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;

    public StartupInvariantValidator(MembershipPlanRepository planRepository, TierRepository tierRepository) {
        this.planRepository = planRepository;
        this.tierRepository = tierRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        long activePlans = planRepository.findByActiveTrue().size();
        if (activePlans == 0) {
            throw new IllegalStateException("Startup invariant violated: no active membership plans configured");
        }

        for (BillingCycle cycle : BillingCycle.values()) {
            long count = planRepository.countByBillingCycleAndActiveTrue(cycle);
            if (count > 1) {
                throw new IllegalStateException(
                        "Startup invariant violated: more than one active plan for billing cycle " + cycle);
            }
        }

        long baseTiers = tierRepository.countByActiveTrueAndRank(0);
        if (baseTiers != 1) {
            throw new IllegalStateException(
                    "Startup invariant violated: expected exactly one active base (rank 0) tier but found " + baseTiers);
        }

        log.info("Startup invariants OK: {} active plans, exactly one base tier", activePlans);
    }
}
