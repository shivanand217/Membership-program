package com.firstclub.membership.application;

import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.enums.BillingKind;
import com.firstclub.membership.domain.enums.ChangeType;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.exception.DuplicateActiveSubscriptionException;
import com.firstclub.membership.domain.exception.InvalidMembershipRequestException;
import com.firstclub.membership.domain.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.BillingTransaction;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.SubscriptionEvent;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.pricing.PricingService;
import com.firstclub.membership.repository.AppUserRepository;
import com.firstclub.membership.repository.SubscriptionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Orchestrates the subscription lifecycle: subscribe, upgrade, downgrade, cancel, and criteria-based
 * tier re-evaluation. The aggregate ({@link Subscription}) owns the state rules and audit trail; this
 * service coordinates pricing, tier evaluation and persistence.
 *
 * <h3>Concurrency</h3>
 * The mutating use cases are wrapped in {@link Retryable} on {@link OptimisticLockingFailureException},
 * with each attempt executed in a <em>fresh</em> transaction via {@link TransactionTemplate} so it
 * re-reads the aggregate and re-checks its guards. Subscribe relies on the database
 * one-active-per-user constraint: a lost race surfaces as a {@link DataIntegrityViolationException},
 * translated to a 409.
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository userRepository;
    private final MembershipCatalogService catalog;
    private final PricingService pricing;
    private final TierEvaluationService tierEvaluation;
    private final OrderStatsService orderStats;
    private final MembershipProperties properties;
    private final TransactionTemplate txTemplate;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               AppUserRepository userRepository,
                               MembershipCatalogService catalog,
                               PricingService pricing,
                               TierEvaluationService tierEvaluation,
                               OrderStatsService orderStats,
                               MembershipProperties properties,
                               PlatformTransactionManager txManager) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.catalog = catalog;
        this.pricing = pricing;
        this.tierEvaluation = tierEvaluation;
        this.orderStats = orderStats;
        this.properties = properties;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // ---- Subscribe (fresh transaction; DB constraint guards the one-active invariant) ----

    public Subscription subscribe(Long userId, String planCode, String tierCode, String idempotencyKey) {
        String key = emptyToNull(idempotencyKey);
        try {
            return txTemplate.execute(status -> doSubscribe(userId, planCode, tierCode, key));
        } catch (DataIntegrityViolationException raceLost) {
            // Lost a concurrent race on ux_one_active_per_user / ux_idempotency. If the caller supplied an
            // idempotency key, the winner IS their request — replay it instead of returning an error.
            if (key != null) {
                Subscription replay = findIdempotentReplay(userId, key);
                if (replay != null) {
                    return replay;
                }
            }
            throw new DuplicateActiveSubscriptionException(userId);
        }
    }

    private Subscription doSubscribe(Long userId, String planCode, String tierCode, String key) {
        requireUser(userId);

        if (key != null) {
            var replay = subscriptionRepository.findByUserIdAndIdempotencyKey(userId, key);
            if (replay.isPresent()) {
                return initialize(replay.get());
            }
        }
        if (subscriptionRepository.existsByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)) {
            throw new DuplicateActiveSubscriptionException(userId);
        }

        MembershipPlan plan = catalog.requirePlanByCode(planCode);
        Tier selectedTier = catalog.requireTierByLevelCode(tierCode);
        Tier earnedTier = tierEvaluation.computeEarnedTier(orderStats.buildContext(userId));

        Instant now = Instant.now();
        Instant endsAt = periodEnd(now, plan);
        Money price = pricing.quote(plan, selectedTier);

        Subscription subscription = new Subscription(userId, plan, selectedTier, earnedTier,
                now, endsAt, price, true, key);
        subscription.appendEvent(new SubscriptionEvent(ChangeType.SUBSCRIBE,
                null, selectedTier.getLevel(), null, SubscriptionStatus.ACTIVE,
                "subscribed to " + plan.getCode() + " / " + selectedTier.getLevel()));
        subscription.appendTransaction(new BillingTransaction(BillingKind.CHARGE, price,
                ChangeType.SUBSCRIBE, now, "SUBSCRIBE", "initial subscription charge"));

        // saveAndFlush surfaces a unique-constraint violation here (inside this transaction) so the
        // caller's catch can decide between an idempotent replay and a genuine duplicate.
        return initialize(subscriptionRepository.saveAndFlush(subscription));
    }

    private Subscription findIdempotentReplay(Long userId, String key) {
        return txTemplate.execute(status ->
                subscriptionRepository.findByUserIdAndIdempotencyKey(userId, key)
                        .map(this::initialize)
                        .orElse(null));
    }

    // ---- Upgrade: immediate, prorated ----

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 25, multiplier = 2.0))
    public Subscription upgrade(Long subscriptionId, String targetTierCode) {
        return txTemplate.execute(status -> doUpgrade(subscriptionId, targetTierCode));
    }

    private Subscription doUpgrade(Long subscriptionId, String targetTierCode) {
        Subscription subscription = requireActive(subscriptionId);
        Tier target = catalog.requireTierByLevelCode(targetTierCode);
        Tier current = subscription.getSelectedTier();
        if (target.getRank() <= current.getRank()) {
            throw new InvalidMembershipRequestException(
                    "Upgrade target " + target.getLevel() + " must outrank current tier " + current.getLevel());
        }

        Instant now = Instant.now();
        Money delta = pricing.proratedUpgradeDelta(subscription, subscription.getPlan(), target, now);
        Money newPeriodPrice = pricing.quote(subscription.getPlan(), target);

        subscription.changeSelectedTier(target, newPeriodPrice);
        subscription.appendEvent(new SubscriptionEvent(ChangeType.UPGRADE,
                current.getLevel(), target.getLevel(), SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE,
                "prorated upgrade"));
        if (delta.isPositive()) {
            // dedupKey null: multiple upgrades within one period are legitimate and must not collide.
            subscription.appendTransaction(new BillingTransaction(BillingKind.CHARGE, delta,
                    ChangeType.UPGRADE, subscription.getStartsAt(), null, "prorated upgrade charge"));
        }
        return initialize(subscriptionRepository.save(subscription));
    }

    // ---- Downgrade: deferred to period end ----

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 25, multiplier = 2.0))
    public Subscription downgrade(Long subscriptionId, String targetTierCode) {
        return txTemplate.execute(status -> doDowngrade(subscriptionId, targetTierCode));
    }

    private Subscription doDowngrade(Long subscriptionId, String targetTierCode) {
        Subscription subscription = requireActive(subscriptionId);
        Tier target = catalog.requireTierByLevelCode(targetTierCode);
        Tier current = subscription.getSelectedTier();
        if (target.getRank() >= current.getRank()) {
            throw new InvalidMembershipRequestException(
                    "Downgrade target " + target.getLevel() + " must be lower than current tier " + current.getLevel());
        }

        subscription.schedulePendingDowngrade(target);
        subscription.appendEvent(new SubscriptionEvent(ChangeType.DOWNGRADE,
                current.getLevel(), target.getLevel(), SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE,
                "downgrade scheduled to apply at period end"));
        return initialize(subscriptionRepository.save(subscription));
    }

    // ---- Cancel: at period end ----

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 25, multiplier = 2.0))
    public Subscription cancel(Long subscriptionId) {
        return txTemplate.execute(status -> doCancel(subscriptionId));
    }

    private Subscription doCancel(Long subscriptionId) {
        Subscription subscription = requireActive(subscriptionId);
        if (subscription.isCancelAtPeriodEnd()) {
            return initialize(subscription); // already scheduled — idempotent
        }
        subscription.scheduleCancellation(Instant.now());
        subscription.appendEvent(new SubscriptionEvent(ChangeType.CANCEL,
                null, null, SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE,
                "cancellation scheduled at period end"));
        return initialize(subscriptionRepository.save(subscription));
    }

    // ---- Criteria-based tier re-evaluation (system path: writes earnedTier only) ----

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 4,
            backoff = @Backoff(delay = 25, multiplier = 2.0))
    public Subscription evaluateTier(Long userId) {
        return txTemplate.execute(status -> doEvaluateTier(userId));
    }

    private Subscription doEvaluateTier(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription for user", userId));

        Tier earned = tierEvaluation.computeEarnedTier(orderStats.buildContext(userId));
        Tier previous = subscription.getEarnedTier();
        if (earned.getRank() != previous.getRank()) {
            ChangeType changeType = earned.getRank() > previous.getRank()
                    ? ChangeType.AUTO_PROMOTE : ChangeType.AUTO_DEMOTE;
            subscription.assignEarnedTier(earned);
            subscription.appendEvent(new SubscriptionEvent(changeType,
                    previous.getLevel(), earned.getLevel(), SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE,
                    "criteria-based tier re-evaluation"));
            subscriptionRepository.save(subscription);
        }
        return initialize(subscription);
    }

    // ---- Scheduled lifecycle processing (one due subscription, its own transaction) ----

    /**
     * Process a single elapsed subscription: expire it, or renew it into a fresh (re-priced) period,
     * applying any deferred downgrade first. Idempotent by construction — once the status flips or the
     * period advances the subscription no longer matches the "due" predicate, and concurrent double
     * processing is stopped by the optimistic lock (the loser's transaction rolls back).
     */
    @Transactional
    public void processDueSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        if (subscription == null || !subscription.isActive()) {
            return; // already handled
        }
        Instant now = Instant.now();
        if (subscription.getEndsAt().isAfter(now)) {
            return; // no longer due (was renewed/updated concurrently)
        }

        boolean expire = !subscription.isAutoRenew() || subscription.isCancelAtPeriodEnd();
        if (expire) {
            subscription.transitionTo(SubscriptionStatus.EXPIRED);
            subscription.appendEvent(new SubscriptionEvent(ChangeType.EXPIRE, null, null,
                    SubscriptionStatus.ACTIVE, SubscriptionStatus.EXPIRED,
                    subscription.isCancelAtPeriodEnd() ? "expired at period end after cancellation"
                            : "expired (auto-renew disabled)"));
        } else {
            boolean downgradeApplied = subscription.applyPendingDowngradeIfAny();
            Instant newStart = subscription.getEndsAt();
            Instant newEnd = periodEnd(newStart, subscription.getPlan());
            Money newPrice = pricing.quote(subscription.getPlan(), subscription.getSelectedTier());
            subscription.renewInto(newStart, newEnd, newPrice);
            if (downgradeApplied) {
                subscription.appendEvent(new SubscriptionEvent(ChangeType.DOWNGRADE, null,
                        subscription.getSelectedTier().getLevel(), SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE,
                        "scheduled downgrade applied at renewal"));
            }
            subscription.appendEvent(new SubscriptionEvent(ChangeType.RENEW, null, null,
                    SubscriptionStatus.ACTIVE, SubscriptionStatus.ACTIVE, "renewed for the next period"));
            subscription.appendTransaction(new BillingTransaction(BillingKind.CHARGE, newPrice,
                    ChangeType.RENEW, newStart, "RENEW:" + newStart.getEpochSecond(), "renewal charge"));
        }
        subscriptionRepository.save(subscription);
    }

    // ---- Reads ----

    @Transactional(readOnly = true)
    public Subscription getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(this::initialize)
                .orElseThrow(() -> new ResourceNotFoundException("Active membership for user", userId));
    }

    @Transactional(readOnly = true)
    public Subscription getSubscription(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .map(this::initialize)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
    }

    // ---- helpers ----

    private Subscription requireActive(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", subscriptionId));
        if (!subscription.isActive()) {
            throw new InvalidMembershipRequestException(
                    "Subscription " + subscriptionId + " is not active (status=" + subscription.getStatus() + ")");
        }
        return subscription;
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
    }

    private Instant periodEnd(Instant start, MembershipPlan plan) {
        // Calendar-correct: add the period in the business timezone, not a fixed number of days.
        return ZonedDateTime.ofInstant(start, properties.businessZoneId())
                .plus(plan.periodLength())
                .toInstant();
    }

    /** Initialise lazy collections while still in-session so the web layer can map them safely. */
    private Subscription initialize(Subscription subscription) {
        subscription.getEvents().size();
        subscription.getTransactions().size();
        subscription.effectiveTier().getLevel();
        if (subscription.getPendingTier() != null) {
            subscription.getPendingTier().getLevel();
        }
        return subscription;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
