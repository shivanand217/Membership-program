package com.firstclub.membership.repository;

import com.firstclub.membership.domain.model.UserOrderStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserOrderStatsRepository extends JpaRepository<UserOrderStats, Long> {

    Optional<UserOrderStats> findByUserId(Long userId);

    /**
     * Pessimistic-write lock: serialises concurrent order recordings for the same user so the
     * conditional month-roll + increment is race-free (the increment is a read-modify-write that a bare
     * optimistic lock would only detect, not prevent).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from UserOrderStats s where s.userId = :userId")
    Optional<UserOrderStats> findByUserIdForUpdate(@Param("userId") Long userId);
}
