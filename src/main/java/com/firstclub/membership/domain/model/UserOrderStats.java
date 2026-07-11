package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.pricing.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.YearMonth;

/**
 * A per-user projection of ordering behaviour — the sole feed for order-count / order-value tier
 * criteria. Monthly counters are anchored to a {@link YearMonth} in the business timezone and use
 * <strong>reset-on-read</strong>: any access in a later month sees zero without a background job needing
 * to roll them. A missing row is treated as all-zeros (never an NPE) by the service.
 */
@Entity
@Table(name = "user_order_stats", uniqueConstraints = @UniqueConstraint(name = "ux_stats_user", columnNames = "user_id"))
public class UserOrderStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lifetime_order_count", nullable = false)
    private long lifetimeOrderCount = 0;

    @Column(name = "anchor_year", nullable = false)
    private int anchorYear;

    @Column(name = "anchor_month", nullable = false)
    private int anchorMonth;

    @Column(name = "current_month_order_count", nullable = false)
    private int currentMonthOrderCount = 0;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "month_value_amount", nullable = false, precision = 14, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "month_value_currency", nullable = false, length = 3))
    })
    private Money currentMonthOrderValue;

    @Column(name = "last_order_at")
    private Instant lastOrderAt;

    @Column(name = "last_evaluated_at")
    private Instant lastEvaluatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected UserOrderStats() {
    }

    public UserOrderStats(Long userId, YearMonth anchor, String currency) {
        this.userId = userId;
        this.anchorYear = anchor.getYear();
        this.anchorMonth = anchor.getMonthValue();
        this.currentMonthOrderValue = Money.zero(currency);
    }

    @PrePersist
    void onCreate() {
        if (lastEvaluatedAt == null) {
            lastEvaluatedAt = Instant.now();
        }
    }

    private boolean isSameMonth(YearMonth current) {
        return anchorYear == current.getYear() && anchorMonth == current.getMonthValue();
    }

    /** Roll the monthly window forward (reset counters) if the business month has advanced. */
    public void ensureMonth(YearMonth current, String currency) {
        if (!isSameMonth(current)) {
            this.anchorYear = current.getYear();
            this.anchorMonth = current.getMonthValue();
            this.currentMonthOrderCount = 0;
            this.currentMonthOrderValue = Money.zero(currency);
        }
    }

    /** Record one order, rolling the month window first so counters never bleed across months. */
    public void recordOrder(Money amount, Instant occurredAt, YearMonth current) {
        ensureMonth(current, amount.getCurrency());
        this.lifetimeOrderCount += 1;
        this.currentMonthOrderCount += 1;
        this.currentMonthOrderValue = this.currentMonthOrderValue.add(amount);
        this.lastOrderAt = occurredAt;
        this.lastEvaluatedAt = Instant.now();
    }

    // ---- reset-on-read views used by the criteria engine ----

    public long ordersThisMonth(YearMonth current) {
        return isSameMonth(current) ? currentMonthOrderCount : 0;
    }

    public Money valueThisMonth(YearMonth current, String currency) {
        return isSameMonth(current) ? currentMonthOrderValue : Money.zero(currency);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public long getLifetimeOrderCount() {
        return lifetimeOrderCount;
    }

    public int getAnchorYear() {
        return anchorYear;
    }

    public int getAnchorMonth() {
        return anchorMonth;
    }

    public int getCurrentMonthOrderCount() {
        return currentMonthOrderCount;
    }

    public Money getCurrentMonthOrderValue() {
        return currentMonthOrderValue;
    }

    public Instant getLastOrderAt() {
        return lastOrderAt;
    }

    public Instant getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
