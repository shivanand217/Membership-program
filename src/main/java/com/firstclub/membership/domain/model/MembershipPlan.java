package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.pricing.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Period;

/**
 * A billing option: a cadence ({@link BillingCycle}) plus a base price. Deliberately holds no benefit
 * or tier information — those live on {@code Tier}. The period a purchase covers is always derived from
 * the cycle, never stored as a number of days.
 */
@Entity
@Table(name = "membership_plan", uniqueConstraints = @UniqueConstraint(name = "ux_plan_code", columnNames = "code"))
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)),
            @AttributeOverride(name = "currency", column = @Column(name = "price_currency", nullable = false, length = 3))
    })
    private Money price;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected MembershipPlan() {
    }

    public MembershipPlan(String code, String name, BillingCycle billingCycle, Money price) {
        this.code = code;
        this.name = name;
        this.billingCycle = billingCycle;
        this.price = price;
    }

    /** The length of one billing period for this plan. */
    public Period periodLength() {
        return billingCycle.getPeriod();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public Money getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
