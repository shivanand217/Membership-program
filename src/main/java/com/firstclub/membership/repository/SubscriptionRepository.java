package com.firstclub.membership.repository;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    boolean existsByUserIdAndStatus(Long userId, SubscriptionStatus status);

    /** Idempotency lookup: has this (user, key) already produced a subscription? */
    Optional<Subscription> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /**
     * Ids of subscriptions whose period has elapsed and are still ACTIVE — the work list for the
     * lifecycle sweep. Returning ids (not entities) lets each be processed in its own transaction.
     */
    @Query("select s.id from Subscription s where s.status = :status and s.endsAt <= :now order by s.id asc")
    List<Long> findDueIds(@Param("status") SubscriptionStatus status, @Param("now") Instant now);
}
