package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByExternalRef(String externalRef);
}
