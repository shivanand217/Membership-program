package com.firstclub.membership.domain.model;

import com.firstclub.membership.domain.vo.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The configurable attachment of a {@link Benefit} to a {@link Tier}. This is the row an admin edits to
 * tune the program: whether the perk is enabled, its ordering ({@code priority}) and its type-specific
 * {@code params} (percentage, caps, categories, SLA minutes...) stored as JSON — so new parameters need
 * no schema change.
 *
 * <p>The typed {@code *Param} accessors centralise the messy {@code Object}-to-type coercion that comes
 * from JSON deserialization (numbers arrive as {@code Integer}/{@code Double}/{@code Long}), keeping the
 * benefit handlers clean.
 */
@Entity
@Table(
        name = "tier_benefit",
        uniqueConstraints = @UniqueConstraint(name = "ux_tier_benefit", columnNames = {"tier_id", "benefit_id"})
)
public class TierBenefit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tier_id", nullable = false)
    private Tier tier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "benefit_id", nullable = false)
    private Benefit benefit;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /** Lower runs first when benefits are applied to an order. */
    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "params_json", length = 2000)
    private Map<String, Object> params = new HashMap<>();

    protected TierBenefit() {
    }

    public TierBenefit(Benefit benefit, boolean enabled, int priority, Map<String, Object> params) {
        this.benefit = benefit;
        this.enabled = enabled;
        this.priority = priority;
        this.params = params == null ? new HashMap<>() : new HashMap<>(params);
    }

    // ---- typed parameter accessors (robust to JSON numeric types) ----

    public Optional<BigDecimal> decimalParam(String key) {
        Object v = params.get(key);
        if (v == null) {
            return Optional.empty();
        }
        if (v instanceof Number n) {
            return Optional.of(new BigDecimal(n.toString()));
        }
        return Optional.of(new BigDecimal(v.toString()));
    }

    public Optional<Integer> intParam(String key) {
        return decimalParam(key).map(BigDecimal::intValue);
    }

    public String stringParam(String key, String defaultValue) {
        Object v = params.get(key);
        return v == null ? defaultValue : v.toString();
    }

    /** Reads a JSON array parameter as a list of strings; empty list if absent. */
    public List<String> stringListParam(String key) {
        Object v = params.get(key);
        if (v == null) {
            return List.of();
        }
        if (v instanceof Collection<?> c) {
            List<String> out = new ArrayList<>(c.size());
            for (Object o : c) {
                out.add(String.valueOf(o));
            }
            return out;
        }
        return List.of(v.toString());
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

    public Benefit getBenefit() {
        return benefit;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public Map<String, Object> getParams() {
        return params;
    }
}
