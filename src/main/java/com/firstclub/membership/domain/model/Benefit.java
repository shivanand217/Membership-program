package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.enums.BenefitType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A catalog definition of a benefit — its identity, type and human-readable copy. The actual
 * per-tier parameters (percentage, caps, categories, ...) live on {@link TierBenefit}, so the same
 * benefit definition can be attached to multiple tiers with different settings.
 */
@Entity
@Table(name = "benefit", uniqueConstraints = @UniqueConstraint(name = "ux_benefit_code", columnNames = "code"))
public class Benefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BenefitType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Benefit() {
    }

    public Benefit(String code, BenefitType type, String name, String description) {
        this.code = code;
        this.type = type;
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public BenefitType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
