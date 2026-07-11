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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Applies an extra percentage discount, optionally scoped to categories and capped by {@code maxDiscount}.
 * The per-benefit cap is enforced here; the global cap on stacked percentage discounts is applied by the
 * evaluation service that stacks these outcomes.
 *
 * <p>Params: {@code percentage} (0..100), optional {@code maxDiscount}, optional {@code categories}
 * (list; {@code "all"} or absent = every category).
 */
@Component
public class PercentageDiscountHandler implements BenefitHandler {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Override
    public BenefitType type() {
        return BenefitType.PERCENTAGE_DISCOUNT;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return ctx.hasOrder();
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        OrderPreview order = ctx.order();
        String currency = order.subtotal().getCurrency();

        List<String> categories = def.stringListParam("categories");
        if (!appliesToCategory(categories, order.categoryOrAll())) {
            return BenefitOutcome.notApplied(type(), "category '" + order.categoryOrAll() + "' not eligible");
        }

        BigDecimal percentage = def.decimalParam("percentage").orElse(BigDecimal.ZERO);
        if (percentage.signum() <= 0) {
            return BenefitOutcome.notApplied(type(), "no percentage configured");
        }

        BigDecimal rawDiscount = order.subtotal().getAmount()
                .multiply(percentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // Cap by per-benefit maxDiscount (if set), never exceed the subtotal, and never go negative
        // (a mis-configured negative maxDiscount must not turn a discount into a surcharge).
        BigDecimal capped = def.decimalParam("maxDiscount")
                .map(max -> rawDiscount.min(max))
                .orElse(rawDiscount)
                .min(order.subtotal().getAmount())
                .max(BigDecimal.ZERO);

        Money discount = Money.of(capped, currency);
        return BenefitOutcome.monetary(type(), discount, Map.of(
                "percentage", percentage,
                "rawDiscount", rawDiscount,
                "appliedDiscount", capped,
                "categories", categories.isEmpty() ? List.of("all") : categories
        ), percentage + "% discount applied");
    }

    private boolean appliesToCategory(List<String> categories, String orderCategory) {
        if (categories.isEmpty() || categories.contains("all")) {
            return true;
        }
        return categories.contains(orderCategory);
    }
}
