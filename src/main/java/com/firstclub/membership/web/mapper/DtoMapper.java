package com.firstclub.membership.web.mapper;

import com.firstclub.membership.application.BenefitPreviewResult;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.eligibility.TierQualification;
import com.firstclub.membership.domain.model.BillingTransaction;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.SubscriptionEvent;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.domain.model.UserOrderStats;
import com.firstclub.membership.web.dto.response.BenefitPreviewResponse;
import com.firstclub.membership.web.dto.response.MembershipResponse;
import com.firstclub.membership.web.dto.response.MoneyDto;
import com.firstclub.membership.web.dto.response.OrderStatsView;
import com.firstclub.membership.web.dto.response.PlanResponse;
import com.firstclub.membership.web.dto.response.SubscriptionResponse;
import com.firstclub.membership.web.dto.response.TierEligibilityResponse;
import com.firstclub.membership.web.dto.response.TierResponse;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * Translates domain entities and application results into transport DTOs. Isolating this mapping keeps
 * JPA entities from leaking onto the wire and keeps controllers thin.
 */
@Component
public class DtoMapper {

    public PlanResponse toPlanResponse(MembershipPlan plan) {
        return new PlanResponse(plan.getCode(), plan.getName(),
                plan.getBillingCycle().name(), MoneyDto.from(plan.getPrice()));
    }

    public TierResponse toTierResponse(Tier tier) {
        return new TierResponse(
                tier.getLevel().name(),
                tier.getName(),
                tier.getRank(),
                tier.getPriceMultiplier(),
                tier.getCriteriaMatch().name(),
                tier.getDescription(),
                toBenefitViews(tier),
                tier.getCriteria().stream()
                        .map(this::toCriterionView)
                        .toList());
    }

    public List<TierResponse.BenefitView> toBenefitViews(Tier tier) {
        return tier.getBenefits().stream()
                .sorted(Comparator.comparingInt(TierBenefit::getPriority))
                .map(this::toBenefitView)
                .toList();
    }

    private TierResponse.BenefitView toBenefitView(TierBenefit tb) {
        return new TierResponse.BenefitView(
                tb.getBenefit().getType().name(),
                tb.getBenefit().getName(),
                tb.isEnabled(),
                tb.getPriority(),
                tb.getParams());
    }

    private TierResponse.CriterionView toCriterionView(TierCriterion c) {
        return new TierResponse.CriterionView(c.getType().name(), c.getThreshold(), c.getCohortCode());
    }

    public SubscriptionResponse toSubscriptionResponse(Subscription s) {
        return new SubscriptionResponse(
                s.getId(),
                s.getUserId(),
                s.getPlan().getCode(),
                s.getPlan().getBillingCycle().name(),
                s.getStatus().name(),
                s.getSelectedTier().getLevel().name(),
                s.getEarnedTier().getLevel().name(),
                s.effectiveTier().getLevel().name(),
                s.getPendingTier() == null ? null : s.getPendingTier().getLevel().name(),
                MoneyDto.from(s.getPricePaid()),
                s.getStartsAt(),
                s.getEndsAt(),
                s.isAutoRenew(),
                s.isCancelAtPeriodEnd(),
                s.getEvents().stream().map(this::toEventView).toList(),
                s.getTransactions().stream().map(this::toTransactionView).toList());
    }

    public MembershipResponse toMembershipResponse(Subscription s) {
        return new MembershipResponse(
                s.getUserId(),
                s.getId(),
                s.getStatus().name(),
                s.getPlan().getCode(),
                s.getPlan().getBillingCycle().name(),
                s.effectiveTier().getLevel().name(),
                s.getEarnedTier().getLevel().name(),
                s.getSelectedTier().getLevel().name(),
                s.getPendingTier() == null ? null : s.getPendingTier().getLevel().name(),
                MoneyDto.from(s.getPricePaid()),
                s.getStartsAt(),
                s.getEndsAt(),
                s.isAutoRenew(),
                s.isCancelAtPeriodEnd());
    }

    private SubscriptionResponse.EventView toEventView(SubscriptionEvent e) {
        return new SubscriptionResponse.EventView(
                e.getChangeType().name(),
                e.getFromTier() == null ? null : e.getFromTier().name(),
                e.getToTier() == null ? null : e.getToTier().name(),
                e.getFromStatus() == null ? null : e.getFromStatus().name(),
                e.getToStatus() == null ? null : e.getToStatus().name(),
                e.getReason(),
                e.getOccurredAt());
    }

    private SubscriptionResponse.TransactionView toTransactionView(BillingTransaction t) {
        return new SubscriptionResponse.TransactionView(
                t.getKind().name(),
                MoneyDto.from(t.getAmount()),
                t.getChangeType().name(),
                t.getPeriodStart(),
                t.getOccurredAt(),
                t.getNote());
    }

    public OrderStatsView toOrderStatsView(UserOrderStats stats, YearMonth month, String currency) {
        return new OrderStatsView(
                stats.getUserId(),
                stats.getLifetimeOrderCount(),
                stats.ordersThisMonth(month),
                MoneyDto.from(stats.valueThisMonth(month, currency)),
                stats.getLastOrderAt());
    }

    public TierEligibilityResponse toEligibilityResponse(TierEvaluationContext ctx,
                                                         String earnedTierLevel,
                                                         List<TierQualification> qualifications) {
        OrderStatsView stats = new OrderStatsView(
                ctx.userId(),
                ctx.lifetimeOrderCount(),
                ctx.ordersThisMonth(),
                MoneyDto.from(ctx.spendThisMonth()),
                null);

        List<TierEligibilityResponse.TierQualificationView> views = qualifications.stream()
                .map(q -> new TierEligibilityResponse.TierQualificationView(
                        q.level().name(),
                        q.name(),
                        q.rank(),
                        q.match().name(),
                        q.qualifies(),
                        q.criteria().stream()
                                .map(c -> new TierEligibilityResponse.CriterionEvaluationView(
                                        c.type().name(), c.requirement(), c.satisfied()))
                                .toList()))
                .toList();

        return new TierEligibilityResponse(ctx.userId(), earnedTierLevel, stats,
                ctx.cohorts().stream().sorted().toList(), views);
    }

    public BenefitPreviewResponse toBenefitPreviewResponse(BenefitPreviewResult result) {
        List<BenefitPreviewResponse.BenefitOutcomeView> outcomes = result.outcomes().stream()
                .map(o -> new BenefitPreviewResponse.BenefitOutcomeView(
                        o.type().name(),
                        o.applied(),
                        MoneyDto.from(o.monetaryEffect()),
                        o.description(),
                        o.attributes()))
                .toList();

        return new BenefitPreviewResponse(
                result.tier().name(),
                outcomes,
                MoneyDto.from(result.subtotal()),
                MoneyDto.from(result.deliveryFee()),
                MoneyDto.from(result.discountOnSubtotal()),
                MoneyDto.from(result.deliveryWaived()),
                MoneyDto.from(result.totalDiscount()),
                MoneyDto.from(result.finalPayable()),
                result.deliveryFreed());
    }
}
