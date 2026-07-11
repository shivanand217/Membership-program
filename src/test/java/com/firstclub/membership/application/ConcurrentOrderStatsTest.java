package com.firstclub.membership.application;

import com.firstclub.membership.domain.model.AppUser;
import com.firstclub.membership.domain.pricing.Money;
import com.firstclub.membership.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the get-or-create race in {@link OrderStatsService#recordOrder}: two simultaneous first orders
 * for a brand-new user (no stats row yet) must both be counted — the loser of the insert race retries in a
 * fresh transaction and updates the now-existing row rather than losing its order.
 */
@SpringBootTest
class ConcurrentOrderStatsTest {

    @Autowired
    private OrderStatsService orderStatsService;
    @Autowired
    private AppUserRepository userRepository;

    @Test
    @DisplayName("two concurrent first orders for a new user are both counted")
    void concurrentFirstOrdersBothCounted() throws Exception {
        AppUser user = userRepository.save(new AppUser("stats-race", "stats-race@firstclub.test", "Stats Race"));
        long userId = user.getId();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        Callable<Void> task = () -> {
            ready.countDown();
            go.await();
            orderStatsService.recordOrder(userId, Money.inr(100), Instant.now());
            return null;
        };

        Future<Void> f1 = pool.submit(task);
        Future<Void> f2 = pool.submit(task);
        ready.await();
        go.countDown();
        f1.get();
        f2.get();
        pool.shutdown();

        assertThat(orderStatsService.findStats(userId)).isPresent();
        assertThat(orderStatsService.findStats(userId).get().getLifetimeOrderCount()).isEqualTo(2);
    }
}
