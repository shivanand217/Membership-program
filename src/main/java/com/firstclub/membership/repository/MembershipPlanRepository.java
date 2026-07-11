package com.firstclub.membership.repository;

import com.firstclub.membership.domain.enums.BillingCycle;
import com.firstclub.membership.domain.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findByActiveTrue();

    Optional<MembershipPlan> findByCode(String code);

    Optional<MembershipPlan> findByCodeAndActiveTrue(String code);

    long countByBillingCycleAndActiveTrue(BillingCycle billingCycle);
}
