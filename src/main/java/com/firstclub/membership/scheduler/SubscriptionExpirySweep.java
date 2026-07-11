package com.firstclub.membership.scheduler;

import com.firstclub.membership.application.SubscriptionService;
import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.repository.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Periodically finds subscriptions whose period has elapsed and processes each in its own transaction
 * (expire or renew). Processing per-id in a fresh transaction keeps one bad row from failing the batch and
 * lets the optimistic lock arbitrate if the work is ever picked up twice.
 *
 * <p>Single-node design; a distributed lock (e.g. ShedLock) would be the one-line addition to run it on a
 * cluster. That seam is intentionally not built for this assignment.
 */
@Component
public class SubscriptionExpirySweep {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpirySweep.class);

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final MembershipProperties properties;

    public SubscriptionExpirySweep(SubscriptionRepository subscriptionRepository,
                                   SubscriptionService subscriptionService,
                                   MembershipProperties properties) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.properties = properties;
    }

    @Scheduled(cron = "${membership.expiry.sweep-cron}")
    public void scheduledSweep() {
        if (!properties.getExpiry().isEnabled()) {
            return;
        }
        sweepOnce();
    }

    /**
     * Run one pass. Exposed (returns a small summary) so integration tests can drive it deterministically
     * instead of waiting for the cron.
     *
     * @return number of due subscriptions actually processed
     */
    public int sweepOnce() {
        Instant now = Instant.now();
        List<Long> dueIds = subscriptionRepository.findDueIds(SubscriptionStatus.ACTIVE, now);
        if (dueIds.isEmpty()) {
            return 0;
        }

        int processed = 0;
        int skipped = 0;
        for (Long id : dueIds) {
            try {
                subscriptionService.processDueSubscription(id);
                processed++;
            } catch (OptimisticLockingFailureException e) {
                skipped++; // concurrently handled elsewhere — safe to ignore
            } catch (RuntimeException e) {
                log.warn("Lifecycle sweep failed for subscription {}: {}", id, e.toString());
            }
        }
        log.info("Lifecycle sweep: {} due, {} processed, {} skipped", dueIds.size(), processed, skipped);
        return processed;
    }
}
