package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BillingKind;
import com.firstclub.membership.domain.enums.ChangeType;
import com.firstclub.membership.domain.pricing.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A money-movement ledger entry. The nullable {@code dedup_key} (unique per subscription) makes
 * <em>once-per-period</em> charges idempotent: a re-run renewal or a replayed subscribe carries the same
 * key and its second insert is rejected. Charges that may legitimately recur within a period (e.g. two
 * upgrades) carry a {@code null} key — SQL treats NULLs as distinct, so they never collide.
 */
@Entity
@Table(
        name = "billing_transaction",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_billing_dedup",
                columnNames = {"subscription_id", "dedup_key"})
)
public class BillingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private BillingKind kind;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "amount", nullable = false, precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    /** Idempotency key for once-per-period charges; null for charges that may recur within a period. */
    @Column(name = "dedup_key")
    private String dedupKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "note")
    private String note;

    protected BillingTransaction() {
    }

    public BillingTransaction(BillingKind kind, Money amount, ChangeType changeType,
                              Instant periodStart, String dedupKey, String note) {
        this.kind = kind;
        this.amount = amount;
        this.changeType = changeType;
        this.periodStart = periodStart;
        this.dedupKey = dedupKey;
        this.note = note;
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

    public BillingKind getKind() {
        return kind;
    }

    public Money getAmount() {
        return amount;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getNote() {
        return note;
    }
}
