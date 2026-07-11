package com.firstclub.membership.repository;

import com.firstclub.membership.domain.enums.TierLevel;
import com.firstclub.membership.domain.model.Tier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TierRepository extends JpaRepository<Tier, Long> {

    /** Ascending rank (base first) — used for catalog listing. */
    List<Tier> findByActiveTrueOrderByRankAsc();

    /** Descending rank (highest first) — used by the evaluator to pick the best qualifying tier. */
    List<Tier> findByActiveTrueOrderByRankDesc();

    Optional<Tier> findByLevel(TierLevel level);

    Optional<Tier> findByLevelAndActiveTrue(TierLevel level);

    /** Base tier(s): the always-eligible rank-0 tier. Startup asserts exactly one. */
    long countByActiveTrueAndRank(int rank);
}
