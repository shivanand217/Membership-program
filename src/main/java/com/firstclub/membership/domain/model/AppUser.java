package com.firstclub.membership.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * A platform user. Subscriptions, order stats and cohort memberships reference the user by id rather
 * than by owned collections, keeping each aggregate small and independently lockable.
 */
@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(name = "ux_user_external_ref", columnNames = "external_ref"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable external identifier (e.g. from the shopping platform). */
    @Column(name = "external_ref", nullable = false)
    private String externalRef;

    @Column(name = "email")
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AppUser() {
    }

    public AppUser(String externalRef, String email, String displayName) {
        this.externalRef = externalRef;
        this.email = email;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
