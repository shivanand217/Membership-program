package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.CriteriaType;
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
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One progression rule attached to a {@link Tier}. All criteria of a tier are ANDed together with a
 * fixed {@code >=} comparison — a deliberately flat model (no rule DSL, no grouping) that still covers
 * every stated requirement and is trivial to extend by adding a {@link CriteriaType} + a rule strategy.
 */
@Entity
@Table(name = "tier_criterion")
public class TierCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private CriteriaType type;

    /** Numeric threshold for count/value criteria; null for cohort criteria. */
    @Column(name = "threshold", precision = 14, scale = 2)
    private BigDecimal threshold;

    /** Required cohort code for {@link CriteriaType#COHORT_MEMBERSHIP}; null otherwise. */
    @Column(name = "cohort_code")
    private String cohortCode;

    protected TierCriterion() {
    }

    private TierCriterion(CriteriaType type, BigDecimal threshold, String cohortCode) {
        this.type = type;
        this.threshold = threshold;
        this.cohortCode = cohortCode;
    }

    /** Factory for a numeric (count or value) criterion. */
    public static TierCriterion numeric(CriteriaType type, BigDecimal threshold) {
        if (threshold == null) {
            throw new IllegalArgumentException("threshold is required for " + type);
        }
        return new TierCriterion(type, threshold, null);
    }

    /** Factory for a cohort-membership criterion. */
    public static TierCriterion cohort(String cohortCode) {
        if (cohortCode == null || cohortCode.isBlank()) {
            throw new IllegalArgumentException("cohortCode is required for COHORT_MEMBERSHIP");
        }
        return new TierCriterion(CriteriaType.COHORT_MEMBERSHIP, null, cohortCode);
    }

    void assignTier(Tier tier) {
        this.tier = tier;
    }

    public Long getId() {
        return id;
    }

    public Tier getTier() {
        return tier;
    }

    public CriteriaType getType() {
        return type;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public String getCohortCode() {
        return cohortCode;
    }
}
