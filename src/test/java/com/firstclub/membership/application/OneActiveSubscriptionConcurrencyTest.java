package com.firstclub.membership.application;

import com.firstclub.membership.domain.enums.SubscriptionStatus;
import com.firstclub.membership.domain.exception.DuplicateActiveSubscriptionException;
import com.firstclub.membership.domain.model.AppUser;
import com.firstclub.membership.repository.AppUserRepository;
import com.firstclub.membership.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the "at most one active subscription per user" invariant holds under a genuine race: two threads
 * subscribe the same user simultaneously. Exactly one wins; the loser is rejected by the database unique
 * constraint (surfaced as {@link DuplicateActiveSubscriptionException}).
 */
@SpringBootTest
class OneActiveSubscriptionConcurrencyTest {

    @Autowired
    private SubscriptionService subscriptionService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private AppUserRepository userRepository;

    @Test
    @DisplayName("two concurrent subscribes: one succeeds, one is rejected, one active row remains")
    void concurrentSubscribesYieldExactlyOneActive() throws Exception {
        AppUser user = userRepository.save(new AppUser("conc-user", "conc@firstclub.test", "Concurrency User"));
        long userId = user.getId();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        Callable<Void> task = () -> {
            ready.countDown();
            go.await();
            try {
                subscriptionService.subscribe(userId, "MONTHLY", "SILVER", null);
                success.incrementAndGet();
            } catch (DuplicateActiveSubscriptionException e) {
                rejected.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = pool.submit(task);
        Future<Void> f2 = pool.submit(task);
        ready.await();
        go.countDown();   // release both at once
        f1.get();
        f2.get();
        pool.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)).isPresent();
    }
}
