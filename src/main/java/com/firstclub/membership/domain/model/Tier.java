package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.CriteriaMatch;
import com.firstclub.membership.domain.enums.TierLevel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A benefit level (Silver / Gold / Platinum) — the configuration aggregate root. Owns its configurable
 * {@link TierBenefit} attachments and its {@link TierCriterion} progression rules.
 *
 * <p>Ordering is by the numeric {@code rank} (0 = base), which is authoritative over enum order. A tier
 * with no criteria is, by definition, the base tier: always eligible. {@code priceMultiplier} lets a
 * higher tier cost more than the plan's base price ({@code price = plan.price × multiplier}).
 */
@Entity
@Table(
        name = "tier",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_tier_rank", columnNames = "tier_rank"),
                @UniqueConstraint(name = "ux_tier_level", columnNames = "level")
        }
)
public class Tier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private TierLevel level;

    @Column(name = "name", nullable = false)
    private String name;

    /** Authoritative ordering; 0 = base tier. */
    @Column(name = "tier_rank", nullable = false)
    private int rank;

    @Column(name = "price_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    /** How this tier's criteria combine (ANY path earns it, or ALL required). */
    @Enumerated(EnumType.STRING)
    @Column(name = "criteria_match", nullable = false)
    private CriteriaMatch criteriaMatch = CriteriaMatch.ANY;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TierBenefit> benefits = new LinkedHashSet<>();

    @OneToMany(mappedBy = "tier", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TierCriterion> criteria = new LinkedHashSet<>();

    protected Tier() {
    }

    public Tier(TierLevel level, String name, int rank, BigDecimal priceMultiplier, String description) {
        this.level = level;
        this.name = name;
        this.rank = rank;
        this.priceMultiplier = priceMultiplier == null ? BigDecimal.ONE : priceMultiplier;
        this.description = description;
    }

    /** Attach a configured benefit, maintaining both sides of the relationship. */
    public TierBenefit addBenefit(TierBenefit tierBenefit) {
        tierBenefit.assignTier(this);
        benefits.add(tierBenefit);
        return tierBenefit;
    }

    /** Attach a progression criterion, maintaining both sides of the relationship. */
    public TierCriterion addCriterion(TierCriterion criterion) {
        criterion.assignTier(this);
        criteria.add(criterion);
        return criterion;
    }

    public Tier setCriteriaMatch(CriteriaMatch criteriaMatch) {
        this.criteriaMatch = criteriaMatch;
        return this;
    }

    /** A tier with no criteria is the always-eligible base tier. */
    public boolean isBaseTier() {
        return criteria.isEmpty();
    }

    public boolean outranks(Tier other) {
        return this.rank > other.rank;
    }

    public Long getId() {
        return id;
    }

    public TierLevel getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    public BigDecimal getPriceMultiplier() {
        return priceMultiplier;
    }

    public CriteriaMatch getCriteriaMatch() {
        return criteriaMatch;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public Long getVersion() {
        return version;
    }

    public Set<TierBenefit> getBenefits() {
        return benefits;
    }

    public Set<TierCriterion> getCriteria() {
        return criteria;
    }
}
