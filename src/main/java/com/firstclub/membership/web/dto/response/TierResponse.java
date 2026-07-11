package com.firstclub.membership.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A tier and its configuration: the perks it unlocks (with their parameters) and the criteria that earn
 * it for free. Self-describing so the catalog endpoint alone documents the whole program.
 */
public record TierResponse(String level,
                           String name,
                           int rank,
                           BigDecimal priceMultiplier,
                           String criteriaMatch,
                           String description,
                           List<BenefitView> benefits,
                           List<CriterionView> criteria) {

    /** A configured benefit on the tier. */
    public record BenefitView(String benefitType,
                              String name,
                              boolean enabled,
                              int priority,
                              Map<String, Object> params) {
    }

    /** A progression criterion on the tier (all criteria are ANDed). */
    public record CriterionView(String type, BigDecimal threshold, String cohortCode) {
    }
}
