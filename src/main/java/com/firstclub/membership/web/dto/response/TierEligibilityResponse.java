package com.firstclub.membership.web.dto.response;

import java.util.List;

/**
 * Explains a user's tier standing: the tier currently earned, the stats behind it, and a per-tier,
 * per-criterion breakdown of what is and isn't satisfied.
 */
public record TierEligibilityResponse(Long userId,
                                      String earnedTier,
                                      OrderStatsView stats,
                                      List<String> cohorts,
                                      List<TierQualificationView> qualifications) {

    public record TierQualificationView(String level,
                                        String name,
                                        int rank,
                                        String match,
                                        boolean qualifies,
                                        List<CriterionEvaluationView> criteria) {
    }

    public record CriterionEvaluationView(String type, String requirement, boolean satisfied) {
    }
}
