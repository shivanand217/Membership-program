package com.firstclub.membership.application;

import com.firstclub.membership.config.MembershipProperties;
import com.firstclub.membership.domain.benefit.BenefitHandlerRegistry;
import com.firstclub.membership.domain.benefit.handler.EarlyAccessSalesHandler;
import com.firstclub.membership.domain.benefit.handler.ExclusiveCouponsHandler;
import com.firstclub.membership.domain.benefit.handler.ExclusiveDealsHandler;
import com.firstclub.membership.domain.benefit.handler.FreeDeliveryHandler;
import com.firstclub.membership.domain.benefit.handler.PercentageDiscountHandler;
import com.firstclub.membership.domain.benefit.handler.PrioritySupportHandler;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.Benefit;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierBenefit;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.domain.vo.OrderPreview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BenefitEvaluationTest {

    private final BenefitHandlerRegistry registry = new BenefitHandlerRegistry(List.of(
            new FreeDeliveryHandler(), new PercentageDiscountHandler(), new ExclusiveDealsHandler(),
            new EarlyAccessSalesHandler(), new PrioritySupportHandler(), new ExclusiveCouponsHandler()));
    private final MembershipProperties properties = new MembershipProperties(); // globalMax = 1500, INR
    private final BenefitEvaluationService service =
            new BenefitEvaluationService(null, registry, properties);

    private Tier tierWith(TierBenefit... benefits) {
        Tier t = new Tier(TierLevel.GOLD, "Gold", 1, new BigDecimal("1.5"), "");
        for (TierBenefit b : benefits) {
            t.addBenefit(b);
        }
        return t;
    }

    private TierBenefit percentage(int percentage, Integer maxDiscount, List<String> categories) {
        Map<String, Object> params = new HashMap<>();
        params.put("percentage", percentage);
        if (maxDiscount != null) {
            params.put("maxDiscount", maxDiscount);
        }
        params.put("categories", categories);
        return new TierBenefit(new Benefit("PCT", BenefitType.PERCENTAGE_DISCOUNT, "Extra Discount", ""), true, 10, params);
    }

    private TierBenefit freeDelivery(int minOrderValue) {
        return new TierBenefit(new Benefit("FD", BenefitType.FREE_DELIVERY, "Free Delivery", ""),
                true, 5, Map.of("minOrderValue", minOrderValue));
    }

    private OrderPreview order(long subtotal, long delivery, String category) {
        return new OrderPreview(Money.inr(subtotal), Money.inr(delivery), category);
    }

    @Test
    @DisplayName("percentage discount and free delivery combine on an eligible order")
    void percentageAndFreeDeliveryCombine() {
        Tier tier = tierWith(freeDelivery(500), percentage(10, 300, List.of("all")));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(1000, 50, "grocery"));

        assertThat(r.discountOnSubtotal().getAmount()).isEqualByComparingTo("100.00"); // 10% of 1000
        assertThat(r.deliveryWaived().getAmount()).isEqualByComparingTo("50.00");
        assertThat(r.deliveryFreed()).isTrue();
        assertThat(r.totalDiscount().getAmount()).isEqualByComparingTo("150.00");
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("per-benefit maxDiscount caps the percentage discount")
    void perBenefitCapApplies() {
        Tier tier = tierWith(percentage(10, 300, List.of("all")));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(10000, 0, "grocery"));

        assertThat(r.discountOnSubtotal().getAmount()).isEqualByComparingTo("300.00"); // capped from 1000
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("9700.00");
    }

    @Test
    @DisplayName("the global cap bounds the stacked percentage discounts")
    void globalCapApplies() {
        Tier tier = tierWith(percentage(15, 5000, List.of("all")));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(20000, 0, "grocery"));

        // 15% = 3000, under per-benefit cap 5000, but the global cap is 1500.
        assertThat(r.discountOnSubtotal().getAmount()).isEqualByComparingTo("1500.00");
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("18500.00");
    }

    @Test
    @DisplayName("a category-scoped discount does not apply to other categories")
    void categoryScopingExcludesOtherCategories() {
        Tier tier = tierWith(percentage(10, null, List.of("grocery", "fashion")));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(1000, 0, "electronics"));

        assertThat(r.discountOnSubtotal().getAmount()).isEqualByComparingTo("0.00");
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("free delivery does not apply below the minimum order value")
    void freeDeliveryGatedByMinimum() {
        Tier tier = tierWith(freeDelivery(500));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(300, 50, "grocery"));

        assertThat(r.deliveryFreed()).isFalse();
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("350.00");
    }

    @Test
    @DisplayName("waived delivery is capped at the actual delivery fee even if two free-delivery perks stack")
    void deliveryWaivedCappedAtFee() {
        Tier tier = tierWith(freeDelivery(0), freeDelivery(0)); // two FREE_DELIVERY benefits

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(1000, 50, "grocery"));

        assertThat(r.deliveryWaived().getAmount()).isEqualByComparingTo("50.00"); // not 100
        assertThat(r.totalDiscount().getAmount()).isEqualByComparingTo("50.00");
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("a mis-configured negative maxDiscount never turns a discount into a surcharge")
    void negativeMaxDiscountNeverOvercharges() {
        Tier tier = tierWith(percentage(10, -100, List.of("all")));

        BenefitPreviewResult r = service.applyToOrder(1L, tier, order(1000, 0, "grocery"));

        assertThat(r.discountOnSubtotal().getAmount()).isEqualByComparingTo("0.00");
        assertThat(r.finalPayable().getAmount()).isEqualByComparingTo("1000.00"); // not 1100
    }
}
