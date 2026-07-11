package com.firstclub.membership.web.controller;

import com.firstclub.membership.application.OrderStatsService;
import com.firstclub.membership.application.SubscriptionService;
import com.firstclub.membership.application.TierEvaluationService;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.eligibility.TierQualification;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.web.dto.response.MembershipResponse;
import com.firstclub.membership.web.dto.response.TierEligibilityResponse;
import com.firstclub.membership.web.mapper.DtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@Tag(name = "Membership", description = "Track current membership and tier standing")
public class MembershipController {

    private final SubscriptionService subscriptionService;
    private final TierEvaluationService tierEvaluation;
    private final OrderStatsService orderStats;
    private final DtoMapper mapper;

    public MembershipController(SubscriptionService subscriptionService,
                                TierEvaluationService tierEvaluation,
                                OrderStatsService orderStats,
                                DtoMapper mapper) {
        this.subscriptionService = subscriptionService;
        this.tierEvaluation = tierEvaluation;
        this.orderStats = orderStats;
        this.mapper = mapper;
    }

    @GetMapping("/membership")
    @Operation(summary = "Current membership", description = "Plan, effective tier, price and expiry")
    public MembershipResponse membership(@PathVariable Long userId) {
        return mapper.toMembershipResponse(subscriptionService.getActiveSubscription(userId));
    }

    @GetMapping("/tier-eligibility")
    @Operation(summary = "Tier eligibility", description = "Which tiers the user qualifies for, with per-criterion reasons")
    public TierEligibilityResponse tierEligibility(@PathVariable Long userId) {
        TierEvaluationContext ctx = orderStats.buildContext(userId);
        Tier earned = tierEvaluation.computeEarnedTier(ctx);
        List<TierQualification> qualifications = tierEvaluation.explain(ctx);
        return mapper.toEligibilityResponse(ctx, earned.getLevel().name(), qualifications);
    }

    @PostMapping("/tier/evaluate")
    @Operation(summary = "Re-evaluate tier now",
            description = "Runs the criteria engine and applies the earned tier (free; never re-prices)")
    public MembershipResponse evaluateTier(@PathVariable Long userId) {
        return mapper.toMembershipResponse(subscriptionService.evaluateTier(userId));
    }
}
