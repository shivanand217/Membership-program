package com.firstclub.membership.application;

import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.MembershipPlan;
import com.firstclub.membership.domain.model.Tier;
import com.firstclub.membership.repository.MembershipPlanRepository;
import com.firstclub.membership.repository.TierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Read facade over the plan/tier catalog. Also the single place that resolves the public codes
 * (plan {@code code}, tier {@code level}) used in requests into entities, so validation lives in one spot.
 *
 * <p>Because {@code open-in-view} is disabled, the list methods deliberately initialise each tier's lazy
 * {@code benefits}/{@code criteria} inside the transaction so the web layer can map them safely.
 */
@Service
public class MembershipCatalogService {

    private final MembershipPlanRepository planRepository;
    private final TierRepository tierRepository;

    public MembershipCatalogService(MembershipPlanRepository planRepository, TierRepository tierRepository) {
        this.planRepository = planRepository;
        this.tierRepository = tierRepository;
    }

    @Transactional(readOnly = true)
    public List<MembershipPlan> listActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparingLong(p -> p.periodLength().toTotalMonths()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Tier> listActiveTiers() {
        List<Tier> tiers = tierRepository.findByActiveTrueOrderByRankAsc();
        tiers.forEach(this::initialize);
        return tiers;
    }

    @Transactional(readOnly = true)
    public Tier requireTierByLevelCode(String levelCode) {
        Tier tier = tierRepository.findByLevelAndActiveTrue(parseLevel(levelCode))
                .orElseThrow(() -> new ResourceNotFoundException("Tier", levelCode));
        initialize(tier);
        return tier;
    }

    @Transactional(readOnly = true)
    public MembershipPlan requirePlanByCode(String code) {
        return planRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", code));
    }

    /** The always-eligible base tier (lowest rank). */
    @Transactional(readOnly = true)
    public Tier requireBaseTier() {
        return tierRepository.findByActiveTrueOrderByRankAsc().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active tiers configured"));
    }

    private void initialize(Tier tier) {
        tier.getBenefits().forEach(tb -> tb.getBenefit().getName());
        tier.getCriteria().size();
    }

    private TierLevel parseLevel(String levelCode) {
        try {
            return TierLevel.valueOf(levelCode.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Treat an unknown tier code as "not found", consistent with an unknown plan code (both 404).
            throw new ResourceNotFoundException("Tier", levelCode);
        }
    }
}
