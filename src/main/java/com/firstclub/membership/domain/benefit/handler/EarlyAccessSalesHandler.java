package com.firstclub.membership.domain.benefit.handler;

import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Informational perk: early access to sales windows. Params: optional {@code earlyAccessHours}. */
@Component
public class EarlyAccessSalesHandler implements BenefitHandler {

    @Override
    public BenefitType type() {
        return BenefitType.EARLY_ACCESS_SALES;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return true;
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        int hours = def.intParam("earlyAccessHours").orElse(24);
        return BenefitOutcome.informational(type(),
                Map.of("earlyAccessHours", hours),
                hours + "h early access to sales");
    }
}
