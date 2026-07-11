package com.firstclub.membership.application;

import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitHandlerRegistry;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.vo.OrderPreview;
import com.firstclub.membership.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves a member's configured benefits against a sample order and rolls the outcomes up under the
 * stacking policy:
 * <ul>
 *   <li>percentage discounts stack <em>additively</em>, each already capped by its own {@code maxDiscount};</li>
 *   <li>their sum is then capped by one <em>global</em> cap and by the subtotal;</li>
 *   <li>free delivery is independent;</li>
 *   <li>the final payable is floored at zero.</li>
 * </ul>
 */
@Service
public class BenefitEvaluationService {

    private final SubscriptionRepository subscriptionRepository;
    private final BenefitHandlerRegistry registry;
    private final MembershipProperties properties;

    public BenefitEvaluationService(SubscriptionRepository subscriptionRepository,
                                    BenefitHandlerRegistry registry,
                                    MembershipProperties properties) {
        this.subscriptionRepository = subscriptionRepository;
        this.registry = registry;
        this.properties = properties;
    }

    /** Preview benefits for a user's effective tier (requires an active subscription). */
    @Transactional(readOnly = true)
    public BenefitPreviewResult preview(Long userId, OrderPreview order) {
        Subscription subscription = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active subscription for user", userId));
        return applyToOrder(userId, subscription.effectiveTier(), order);
    }

    /** Core logic, separated for direct testing. Applies {@code tier}'s benefits to {@code order}. */
    @Transactional(readOnly = true)
    public BenefitPreviewResult applyToOrder(Long userId, Tier tier, OrderPreview order) {
        BenefitContext ctx = new BenefitContext(userId, tier, order, Instant.now());
        String currency = order.subtotal().getCurrency();

        List<TierBenefit> enabled = tier.getBenefits().stream()
                .filter(TierBenefit::isEnabled)
                .sorted(Comparator.comparingInt(TierBenefit::getPriority))
                .toList();

        List<BenefitOutcome> outcomes = new ArrayList<>();
        Money summedPercentage = Money.zero(currency);
        Money deliveryWaived = Money.zero(currency);

        for (TierBenefit tierBenefit : enabled) {
            BenefitType type = tierBenefit.getBenefit().getType();
            BenefitHandler handler = registry.require(type);
            BenefitOutcome outcome = handler.supports(ctx)
                    ? handler.apply(ctx, tierBenefit)
                    : BenefitOutcome.notApplied(type, "not applicable without an order");
            outcomes.add(outcome);

            if (outcome.applied()) {
                if (type == BenefitType.PERCENTAGE_DISCOUNT) {
                    summedPercentage = summedPercentage.add(outcome.monetaryEffect());
                } else if (type == BenefitType.FREE_DELIVERY) {
                    deliveryWaived = deliveryWaived.add(outcome.monetaryEffect());
                }
            }
        }

        // Global cap on the stacked percentage discounts; never discount more than the subtotal, and
        // floor at zero so a mis-configured negative discount can never raise the bill above the subtotal.
        Money globalCap = Money.of(properties.getDiscount().getGlobalMax(), currency);
        Money discountOnSubtotal = summedPercentage.min(globalCap).min(order.subtotal()).atLeastZero();

        // Waived delivery can never exceed the actual delivery fee (e.g. if two free-delivery perks stack).
        Money deliveryWaivedCapped = deliveryWaived.min(order.deliveryFee());

        Money payableSubtotal = order.subtotal().subtract(discountOnSubtotal).atLeastZero();
        Money payableDelivery = order.deliveryFee().subtract(deliveryWaivedCapped).atLeastZero();
        Money finalPayable = payableSubtotal.add(payableDelivery);
        Money totalDiscount = discountOnSubtotal.add(deliveryWaivedCapped);

        return new BenefitPreviewResult(tier.getLevel(), outcomes, order.subtotal(), order.deliveryFee(),
                discountOnSubtotal, deliveryWaivedCapped, totalDiscount, finalPayable, deliveryWaivedCapped.isPositive());
    }
}
