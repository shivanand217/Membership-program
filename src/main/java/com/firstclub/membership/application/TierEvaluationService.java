package com.firstclub.membership.application;

import com.firstclub.membership.domain.eligibility.CriterionEvaluation;
import com.firstclub.membership.domain.eligibility.TierEligibilityRule;
import com.firstclub.membership.domain.eligibility.TierEvaluationContext;
import com.firstclub.membership.domain.eligibility.TierQualification;
import com.firstclub.membership.domain.enums.CriteriaType;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.domain.model.TierCriterion;
import com.firstclub.membership.repository.TierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Computes the tier a user has <em>earned</em> from their behaviour. The set of criteria types it can
 * evaluate is exactly the set of {@link TierEligibilityRule} beans on the classpath — dispatched by type,
 * so adding a criterion never touches this class.
 *
 * <p>{@code computeEarnedTier} is a pure function of the passed context and the tier configuration: the
 * highest-ranked tier whose criteria all pass. The base tier (no criteria) always passes, guaranteeing a
 * total function.
 */
@Service
public class TierEvaluationService {

    private final TierRepository tierRepository;
    private final Map<CriteriaType, TierEligibilityRule> rulesByType;

    public TierEvaluationService(TierRepository tierRepository, List<TierEligibilityRule> rules) {
        this.tierRepository = tierRepository;
        this.rulesByType = rules.stream().collect(Collectors.toUnmodifiableMap(
                TierEligibilityRule::type,
                Function.identity(),
                (a, b) -> {
                    throw new IllegalStateException("Duplicate TierEligibilityRule for " + a.type());
                }));
    }

    /** The highest-ranked active tier the user qualifies for (never null: the base tier always qualifies). */
    @Transactional(readOnly = true)
    public Tier computeEarnedTier(TierEvaluationContext ctx) {
        return selectEarnedTier(tierRepository.findByActiveTrueOrderByRankDesc(), ctx);
    }

    /**
     * Pure selection over a caller-supplied tier list (highest rank first). Separated from the repository
     * lookup so the core progression logic is unit-testable without any persistence.
     */
    public Tier selectEarnedTier(List<Tier> tiersHighToLow, TierEvaluationContext ctx) {
        for (Tier tier : tiersHighToLow) {
            if (qualifies(tier, ctx)) {
                return tier;
            }
        }
        throw new IllegalStateException("No eligible tier found — is the base (rank 0) tier configured?");
    }

    /**
     * Whether {@code tier} is earned: the base tier (no criteria) always qualifies; otherwise the tier's
     * {@code criteriaMatch} decides whether ANY one path or ALL criteria must be satisfied.
     */
    public boolean qualifies(Tier tier, TierEvaluationContext ctx) {
        if (tier.getCriteria().isEmpty()) {
            return true;
        }
        return switch (tier.getCriteriaMatch()) {
            case ANY -> tier.getCriteria().stream().anyMatch(c -> ruleFor(c.getType()).isSatisfied(c, ctx));
            case ALL -> tier.getCriteria().stream().allMatch(c -> ruleFor(c.getType()).isSatisfied(c, ctx));
        };
    }

    /** Per-tier, per-criterion breakdown for the eligibility endpoint (tiers ascending by rank). */
    @Transactional(readOnly = true)
    public List<TierQualification> explain(TierEvaluationContext ctx) {
        List<TierQualification> result = new ArrayList<>();
        for (Tier tier : tierRepository.findByActiveTrueOrderByRankAsc()) {
            List<CriterionEvaluation> criteria = new ArrayList<>();
            for (TierCriterion criterion : tier.getCriteria()) {
                boolean satisfied = ruleFor(criterion.getType()).isSatisfied(criterion, ctx);
                criteria.add(new CriterionEvaluation(criterion.getType(), describe(criterion), satisfied));
            }
            result.add(new TierQualification(tier.getId(), tier.getLevel(), tier.getName(),
                    tier.getRank(), tier.getCriteriaMatch(), qualifies(tier, ctx), criteria));
        }
        return result;
    }

    private TierEligibilityRule ruleFor(CriteriaType type) {
        TierEligibilityRule rule = rulesByType.get(type);
        if (rule == null) {
            throw new IllegalStateException("No TierEligibilityRule registered for " + type);
        }
        return rule;
    }

    private String describe(TierCriterion c) {
        return switch (c.getType()) {
            case MIN_LIFETIME_ORDERS -> "lifetime orders >= " + c.getThreshold().toPlainString();
            case MIN_MONTHLY_ORDERS -> "orders this month >= " + c.getThreshold().toPlainString();
            case MIN_MONTHLY_ORDER_VALUE -> "spend this month >= " + c.getThreshold().toPlainString();
            case COHORT_MEMBERSHIP -> "member of cohort '" + c.getCohortCode() + "'";
        };
    }
}
