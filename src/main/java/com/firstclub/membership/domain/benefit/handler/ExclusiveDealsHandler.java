package com.firstclub.membership.domain.benefit.handler;

import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Informational perk: access to members-only deals. */
@Component
public class ExclusiveDealsHandler implements BenefitHandler {

    @Override
    public BenefitType type() {
        return BenefitType.EXCLUSIVE_DEALS;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return true;
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        return BenefitOutcome.informational(type(), Map.of(), "access to exclusive members-only deals");
    }
}
