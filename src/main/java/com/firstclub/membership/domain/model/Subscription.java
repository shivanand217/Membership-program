package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.exception.IllegalStateTransitionException;
import com.firstclub.membership.domain.pricing.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The lifecycle aggregate root: binds a user to a {@link MembershipPlan} and a {@link Tier}, and owns the
 * transitions, event log and billing ledger.
 *
 * <h3>Two-tier model (the key abstraction)</h3>
 * Tier is stored as two independent fields:
 * <ul>
 *   <li>{@code selectedTier} — written only by user actions (subscribe / upgrade / downgrade), and priced.</li>
 *   <li>{@code earnedTier} — written only by the criteria engine, and always free.</li>
 * </ul>
 * The member always gets {@code effectiveTier = max(earnedTier, selectedTier)} by rank. Because neither
 * path writes the other's field, background re-evaluation can never thrash or silently re-charge the user.
 *
 * <h3>Concurrency</h3>
 * {@code @Version} gives optimistic locking for upgrade/downgrade/cancel. {@code activeMarker} (kept equal
 * to {@code userId} only while ACTIVE) carries a unique constraint that enforces "at most one active
 * subscription per user" at the database level.
 */
@Entity
@Table(
        name = "subscription",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_one_active_per_user", columnNames = "active_marker"),
                @UniqueConstraint(name = "ux_idempotency", columnNames = {"user_id", "idempotency_key"})
        }
)
public class Subscription {

    /**
     * State-as-data transition table. ACTIVE -> ACTIVE is allowed and represents an in-place change
     * (tier change or renewal). CANCELLED and EXPIRED are terminal.
     */
    private static final Map<SubscriptionStatus, Set<SubscriptionStatus>> ALLOWED = Map.of(
            SubscriptionStatus.ACTIVE, EnumSet.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED),
            SubscriptionStatus.CANCELLED, EnumSet.noneOf(SubscriptionStatus.class),
            SubscriptionStatus.EXPIRED, EnumSet.noneOf(SubscriptionStatus.class)
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "selected_tier_id", nullable = false)
    private Tier selectedTier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "earned_tier_id", nullable = false)
    private Tier earnedTier;

    /** Deferred downgrade target, applied at the next period boundary. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pending_tier_id")
    private Tier pendingTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd = false;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew = true;

    /** Catalog price of the current selected tier for this period (snapshot). */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_paid_amount", nullable = false, precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "price_paid_currency", nullable = false, length = 3))
    })
    private Money pricePaid;

    /** Equals userId while ACTIVE, NULL otherwise — the column behind the one-active-per-user constraint. */
    @Column(name = "active_marker")
    private Long activeMarker;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC, id ASC")
    private List<SubscriptionEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("occurredAt ASC, id ASC")
    private List<BillingTransaction> transactions = new ArrayList<>();

    protected Subscription() {
    }

    public Subscription(Long userId, MembershipPlan plan, Tier selectedTier, Tier earnedTier,
                        Instant startsAt, Instant endsAt, Money pricePaid,
                        boolean autoRenew, String idempotencyKey) {
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("endsAt must be after startsAt");
        }
        this.userId = userId;
        this.plan = plan;
        this.selectedTier = selectedTier;
        this.earnedTier = earnedTier;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.pricePaid = pricePaid;
        this.autoRenew = autoRenew;
        this.idempotencyKey = idempotencyKey;
        this.status = SubscriptionStatus.ACTIVE;
    }

    // ---- transition mechanism ----

    /** Guarded status change; the single choke point for all lifecycle moves. */
    public void transitionTo(SubscriptionStatus next) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(next)) {
            throw new IllegalStateTransitionException(status, next);
        }
        this.status = next;
    }

    // ---- derived state ----

    /** The tier the member actually enjoys: the higher-ranked of earned and selected. */
    public Tier effectiveTier() {
        if (earnedTier == null) {
            return selectedTier;
        }
        return earnedTier.getRank() >= selectedTier.getRank() ? earnedTier : selectedTier;
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public boolean isDue(Instant now) {
        return isActive() && !endsAt.isAfter(now);
    }

    // ---- user-driven mutations (write selectedTier / lifecycle only) ----

    /**
     * Upgrade: raise the paid (selected) tier immediately and re-snapshot its period price. An explicit
     * tier purchase supersedes any scheduled downgrade, so a pending downgrade is cleared.
     */
    public void changeSelectedTier(Tier newTier, Money newPeriodPrice) {
        this.selectedTier = newTier;
        this.pricePaid = newPeriodPrice;
        this.pendingTier = null;
    }

    /** Downgrade: record the target to be applied at the period boundary (no mid-cycle refund). */
    public void schedulePendingDowngrade(Tier target) {
        this.pendingTier = target;
    }

    /** Cancel-at-period-end: stop future renewals but keep benefits until {@code endsAt}. */
    public void scheduleCancellation(Instant requestedAt) {
        this.cancelAtPeriodEnd = true;
        this.autoRenew = false;
        this.cancelledAt = requestedAt;
    }

    // ---- system-driven mutations ----

    /** Criteria engine only. Free; never touches selectedTier or price. */
    public void assignEarnedTier(Tier tier) {
        this.earnedTier = tier;
    }

    /** Apply a deferred downgrade at a period boundary. Returns true if one was pending. */
    public boolean applyPendingDowngradeIfAny() {
        if (pendingTier == null) {
            return false;
        }
        this.selectedTier = pendingTier;
        this.pendingTier = null;
        return true;
    }

    /** Open a fresh period on renewal, re-priced by the caller from the current catalog. */
    public void renewInto(Instant newStart, Instant newEnd, Money newPrice) {
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("newEnd must be after newStart");
        }
        this.startsAt = newStart;
        this.endsAt = newEnd;
        this.pricePaid = newPrice;
    }

    // ---- owned-collection helpers (maintain both sides) ----

    public SubscriptionEvent appendEvent(SubscriptionEvent event) {
        event.assignSubscription(this);
        events.add(event);
        return event;
    }

    public BillingTransaction appendTransaction(BillingTransaction txn) {
        txn.assignSubscription(this);
        transactions.add(txn);
        return txn;
    }

    // ---- JPA lifecycle: keep audit stamps and the active marker in sync ----

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        syncActiveMarker();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        syncActiveMarker();
    }

    private void syncActiveMarker() {
        this.activeMarker = (status == SubscriptionStatus.ACTIVE) ? userId : null;
    }

    // ---- accessors ----

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public MembershipPlan getPlan() {
        return plan;
    }

    public Tier getSelectedTier() {
        return selectedTier;
    }

    public Tier getEarnedTier() {
        return earnedTier;
    }

    public Tier getPendingTier() {
        return pendingTier;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public Money getPricePaid() {
        return pricePaid;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public List<SubscriptionEvent> getEvents() {
        return events;
    }

    public List<BillingTransaction> getTransactions() {
        return transactions;
    }
}
