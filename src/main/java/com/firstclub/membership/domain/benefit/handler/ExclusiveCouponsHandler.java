package com.firstclub.membership.domain.benefit.handler;

import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Informational perk: a set of exclusive coupons granted to the member. Params: {@code count}. */
@Component
public class ExclusiveCouponsHandler implements BenefitHandler {

    @Override
    public BenefitType type() {
        return BenefitType.EXCLUSIVE_COUPONS;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return true;
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        int count = def.intParam("count").orElse(1);
        return BenefitOutcome.informational(type(),
                Map.of("count", count),
                count + " exclusive coupons available");
    }
}
