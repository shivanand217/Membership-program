package com.firstclub.membership.application;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.model.AppUser;
import com.firstclub.membership.domain.model.Subscription;
import com.firstclub.membership.repository.AppUserRepository;
import com.firstclub.membership.scheduler.SubscriptionExpirySweep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the lifecycle sweep deterministically: a subscription's {@code ends_at} is pushed into the past
 * (simulating time passing), then {@link SubscriptionExpirySweep#sweepOnce()} runs and we assert the
 * expire / renew outcomes.
 */
@SpringBootTest
class LifecycleSweepIntegrationTest {

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionExpirySweep sweep;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private long freshUser(String ref) {
        return userRepository.save(new AppUser(ref, ref + "@firstclub.test", ref)).getId();
    }

    private void setEndsAt(long subscriptionId, Instant when) {
        jdbc.update("update subscription set ends_at = ? where id = ?",
                OffsetDateTime.ofInstant(when, ZoneOffset.UTC), subscriptionId);
    }

    @Test
    @DisplayName("auto-renew off and past due -> EXPIRED")
    void expiresWhenAutoRenewOff() {
        long userId = freshUser("sweep-expire");
        Subscription sub = subscriptionService.subscribe(userId, "MONTHLY", "SILVER", null);
        jdbc.update("update subscription set ends_at = ?, auto_renew = ? where id = ?",
                OffsetDateTime.ofInstant(Instant.now().minus(1, ChronoUnit.HOURS), ZoneOffset.UTC), false, sub.getId());

        int processed = sweep.sweepOnce();

        assertThat(processed).isEqualTo(1);
        assertThat(subscriptionService.getSubscription(sub.getId()).getStatus())
                .isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("auto-renew on and past due -> renewed into a new period with a RENEW charge")
    void renewsWhenAutoRenewOn() {
        long userId = freshUser("sweep-renew");
        Subscription sub = subscriptionService.subscribe(userId, "MONTHLY", "SILVER", null);
        setEndsAt(sub.getId(), Instant.now().minus(1, ChronoUnit.HOURS)); // auto_renew stays true

        sweep.sweepOnce();

        Subscription renewed = subscriptionService.getSubscription(sub.getId());
        assertThat(renewed.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(renewed.getEndsAt()).isAfter(Instant.now());
        assertThat(renewed.getTransactions())
                .anyMatch(t -> t.getChangeType().name().equals("RENEW"));
    }

    @Test
    @DisplayName("cancelled-at-period-end forces EXPIRED even with auto-renew on")
    void cancelAtPeriodEndExpires() {
        long userId = freshUser("sweep-cancel");
        Subscription sub = subscriptionService.subscribe(userId, "MONTHLY", "SILVER", null);
        subscriptionService.cancel(sub.getId());
        setEndsAt(sub.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

        sweep.sweepOnce();

        assertThat(subscriptionService.getSubscription(sub.getId()).getStatus())
                .isEqualTo(SubscriptionStatus.EXPIRED);
    }
}
