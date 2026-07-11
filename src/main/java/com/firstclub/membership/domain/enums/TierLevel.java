package com.firstclub.membership.domain.enums;

/**
 * Named benefit levels. Enum <em>declaration order is not authoritative</em> for ranking — the
 * numeric {@code Tier.rank} column is. This keeps ordering data-driven so a new level can be inserted
 * between existing ones without renumbering an enum.
 */
public enum TierLevel {
    SILVER,
    GOLD,
    PLATINUM
}
