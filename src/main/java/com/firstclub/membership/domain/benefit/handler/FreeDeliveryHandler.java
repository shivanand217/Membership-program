package com.firstclub.membership.domain.benefit.handler;

import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.vo.OrderPreview;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Waives the delivery fee when the order value meets the configured {@code minOrderValue}
 * (default 0 = always free). Independent of percentage discounts.
 */
@Component
public class FreeDeliveryHandler implements BenefitHandler {

    @Override
    public BenefitType type() {
        return BenefitType.FREE_DELIVERY;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return ctx.hasOrder();
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        OrderPreview order = ctx.order();
        BigDecimal minOrderValue = def.decimalParam("minOrderValue").orElse(BigDecimal.ZERO);

        if (order.subtotal().getAmount().compareTo(minOrderValue) < 0) {
            return BenefitOutcome.notApplied(type(),
                    "order subtotal below minimum " + minOrderValue + " for free delivery");
        }
        if (order.deliveryFee().isZero()) {
            return BenefitOutcome.monetary(type(), Money.zero(order.deliveryFee().getCurrency()),
                    Map.of("minOrderValue", minOrderValue), "delivery already free");
        }

        Money waived = order.deliveryFee();
        return BenefitOutcome.monetary(type(), waived,
                Map.of("minOrderValue", minOrderValue, "waivedDeliveryFee", waived.getAmount()),
                "free delivery applied");
    }
}
