package com.firstclub.membership.domain.enums;

/**
 * How a tier's criteria combine. {@code ANY} (the spec's "based on criteria like X, Y, or cohort Z")
 * means satisfying one path earns the tier; {@code ALL} requires every criterion. A tier with no
 * criteria is the always-eligible base tier regardless of this setting.
 */
public enum CriteriaMatch {
    ANY,
    ALL
}
