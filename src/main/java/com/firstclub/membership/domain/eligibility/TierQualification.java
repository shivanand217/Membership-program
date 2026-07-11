package com.firstclub.membership.domain.eligibility;

import com.firstclub.membership.domain.enums.CriteriaMatch;
import com.firstclub.membership.domain.enums.TierLevel;

import java.util.List;

/**
 * Whether a user qualifies for a given tier, plus the per-criterion breakdown. {@code match} says how the
 * criteria combine: with {@code ANY}, satisfying one is enough; with {@code ALL}, every criterion must hold.
 */
public record TierQualification(Long tierId,
                                TierLevel level,
                                String name,
                                int rank,
                                CriteriaMatch match,
                                boolean qualifies,
                                List<CriterionEvaluation> criteria) {
}
