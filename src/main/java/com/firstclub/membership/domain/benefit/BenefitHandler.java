package com.firstclub.membership.domain.benefit;

import com.firstclub.membership.domain.enums.BenefitType;
import com.firstclub.membership.domain.model.TierBenefit;

/**
 * Strategy for one {@link BenefitType}. Implementations are stateless Spring beans, discovered as a
 * {@code List<BenefitHandler>} and indexed by {@link #type()} in the {@link BenefitHandlerRegistry}.
 * A new perk category is added by writing one handler — no switch statements, no edits to existing code.
 */
public interface BenefitHandler {

    /** The benefit type this handler serves (registry key). */
    BenefitType type();

    /** Whether this handler can produce a meaningful outcome for the given context. */
    boolean supports(BenefitContext ctx);

    /**
     * Apply the configured benefit ({@code def} carries the per-tier params) to the context.
     * Must be pure: no persistence, no mutation of the arguments.
     */
    BenefitOutcome apply(BenefitContext ctx, TierBenefit def);
}
