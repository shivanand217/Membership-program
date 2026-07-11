package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.ChangeType;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.enums.TierLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Append-only audit record of one change to a subscription. Tier is snapshotted as a {@link TierLevel}
 * so the history stays truthful even if a tier's configuration later changes. Never mutated after insert.
 */
@Entity
@Table(name = "subscription_event")
public class SubscriptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_tier")
    private TierLevel fromTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_tier")
    private TierLevel toTier;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private SubscriptionStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private SubscriptionStatus toStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected SubscriptionEvent() {
    }

    public SubscriptionEvent(ChangeType changeType, TierLevel fromTier, TierLevel toTier,
                             SubscriptionStatus fromStatus, SubscriptionStatus toStatus, String reason) {
        this.changeType = changeType;
        this.fromTier = fromTier;
        this.toTier = toTier;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }

    void assignSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public TierLevel getFromTier() {
        return fromTier;
    }

    public TierLevel getToTier() {
        return toTier;
    }

    public SubscriptionStatus getFromStatus() {
        return fromStatus;
    }

    public SubscriptionStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
