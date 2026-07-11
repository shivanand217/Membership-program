package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {

    Optional<Cohort> findByCode(String code);
}
