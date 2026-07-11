package com.firstclub.membership.application;

import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.model.UserOrderStats;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.repository.UserCohortMembershipRepository;
import com.firstclub.membership.repository.UserOrderStatsRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owns the {@link UserOrderStats} read-model and assembles the {@link TierEvaluationContext}. This is the
 * bridge between "shopping activity" (orders, cohorts) and "tier progression".
 */
@Service
public class OrderStatsService {

    private final UserOrderStatsRepository statsRepository;
    private final UserCohortMembershipRepository cohortMembershipRepository;
    private final MembershipProperties properties;
    private final TransactionTemplate txTemplate;

    public OrderStatsService(UserOrderStatsRepository statsRepository,
                             UserCohortMembershipRepository cohortMembershipRepository,
                             MembershipProperties properties,
                             PlatformTransactionManager txManager) {
        this.statsRepository = statsRepository;
        this.cohortMembershipRepository = cohortMembershipRepository;
        this.properties = properties;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /** The current month in the configured business timezone — the definition of "this month". */
    public YearMonth currentBusinessMonth() {
        return YearMonth.now(properties.businessZoneId());
    }

    /**
     * Record one order for a user.
     *
     * <p>Concurrency: once the stats row exists, a pessimistic write lock serialises the conditional
     * month-roll + increment. The one gap a row lock cannot close is the very first order for a user —
     * two concurrent inserts race on the {@code ux_stats_user} unique constraint. We handle that by
     * running each attempt in its own transaction and retrying once on the resulting
     * {@link DataIntegrityViolationException}: the retry finds the now-committed row and takes the lock.
     */
    public UserOrderStats recordOrder(Long userId, Money amount, Instant occurredAt) {
        try {
            return applyOrder(userId, amount, occurredAt);
        } catch (DataIntegrityViolationException lostCreateRace) {
            // Another thread inserted the first stats row concurrently; the row now exists — retry once.
            return applyOrder(userId, amount, occurredAt);
        }
    }

    private UserOrderStats applyOrder(Long userId, Money amount, Instant occurredAt) {
        return txTemplate.execute(status -> {
            UserOrderStats stats = statsRepository.findByUserIdForUpdate(userId)
                    .orElseGet(() -> new UserOrderStats(userId, currentBusinessMonth(), properties.getCurrency()));
            stats.recordOrder(amount, occurredAt, currentBusinessMonth());
            return statsRepository.save(stats);
        });
    }

    @Transactional(readOnly = true)
    public Optional<UserOrderStats> findStats(Long userId) {
        return statsRepository.findByUserId(userId);
    }

    /**
     * Build the evaluation context for a user. A missing stats row is treated as all-zeros, and cohort
     * codes are gathered from active memberships.
     */
    @Transactional(readOnly = true)
    public TierEvaluationContext buildContext(Long userId) {
        YearMonth month = currentBusinessMonth();
        String currency = properties.getCurrency();

        Optional<UserOrderStats> maybeStats = statsRepository.findByUserId(userId);
        long lifetime = maybeStats.map(UserOrderStats::getLifetimeOrderCount).orElse(0L);
        long monthlyOrders = maybeStats.map(s -> s.ordersThisMonth(month)).orElse(0L);
        Money monthlySpend = maybeStats.map(s -> s.valueThisMonth(month, currency)).orElse(Money.zero(currency));

        Set<String> cohorts = cohortMembershipRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(m -> m.getCohort().getCode())
                .collect(Collectors.toSet());

        return new TierEvaluationContext(userId, lifetime, monthlyOrders, monthlySpend, cohorts, Instant.now());
    }
}
