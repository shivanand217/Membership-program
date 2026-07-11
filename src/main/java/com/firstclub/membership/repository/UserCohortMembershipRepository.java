package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.UserCohortMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCohortMembershipRepository extends JpaRepository<UserCohortMembership, Long> {

    List<UserCohortMembership> findByUserIdAndActiveTrue(Long userId);
}
