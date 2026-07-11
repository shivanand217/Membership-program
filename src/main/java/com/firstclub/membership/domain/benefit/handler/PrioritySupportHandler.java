package com.firstclub.membership.domain.benefit.handler;

import com.firstclub.membership.domain.benefit.BenefitContext;
import com.firstclub.membership.domain.benefit.BenefitHandler;
import com.firstclub.membership.domain.benefit.BenefitOutcome;
import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Informational perk: priority support with an SLA. Params: {@code slaMinutes}, {@code channel}. */
@Component
public class PrioritySupportHandler implements BenefitHandler {

    @Override
    public BenefitType type() {
        return BenefitType.PRIORITY_SUPPORT;
    }

    @Override
    public boolean supports(BenefitContext ctx) {
        return true;
    }

    @Override
    public BenefitOutcome apply(BenefitContext ctx, TierBenefit def) {
        int slaMinutes = def.intParam("slaMinutes").orElse(60);
        String channel = def.stringParam("channel", "chat");
        return BenefitOutcome.informational(type(),
                Map.of("slaMinutes", slaMinutes, "channel", channel),
                "priority support within " + slaMinutes + " min via " + channel);
    }
}
