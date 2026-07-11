package com.firstclub.membership.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Join row: a user belongs to a cohort. Kept as its own entity (not a {@code @ManyToMany}) so the
 * membership can carry its own attributes (joined-at, active) and be queried directly.
 */
@Entity
@Table(
        name = "user_cohort_membership",
        uniqueConstraints = @UniqueConstraint(name = "ux_user_cohort", columnNames = {"user_id", "cohort_id"})
)
public class UserCohortMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected UserCohortMembership() {
    }

    public UserCohortMembership(Long userId, Cohort cohort) {
        this.userId = userId;
        this.cohort = cohort;
    }

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public boolean isActive() {
        return active;
    }
}
