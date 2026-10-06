package com.nido.api.identity.application.handler;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.application.port.in.DeactivateUserUseCase;
import com.nido.api.identity.domain.model.DeactivateUserCommand;
import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Two administrators acting on one account at the same moment: the second is refused, so its holder is told
 * once. Transaction A is held open after its change, B starts, and A commits once B waits on its lock.
 */
@IntegrationTestConfig
class AdminGestureRacesIT {

    @Autowired DeactivateUserUseCase deactivate;
    @Autowired UserIdentityJpaRepository users;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;

    private UUID carolId;

    @BeforeEach
    void setUp() {
        UserIdentityEntity carol = new UserIdentityEntity();
        String name = "race-" + UUID.randomUUID();
        carol.setUsername(name);
        carol.setEmail(name + "@test.com");
        carol.setRole(Role.USER);
        carol.setActive(true);
        carolId = users.saveAndFlush(carol).getId();
    }

    @Test
    void two_administrators_deactivating_one_account_at_once_deactivate_it_once() throws Exception {
        CountDownLatch aDeactivated = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // A: one administrator's deactivation, caught between its change and its commit.
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                deactivate.deactivate(new DeactivateUserCommand(carolId, UUID.randomUUID(), Role.SUPER_ADMIN));
                aDeactivated.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aDeactivated.await(10, TimeUnit.SECONDS)).isTrue();

            // B: another administrator, who loaded the account while it was still active.
            Future<?> b = pool.submit(() ->
                deactivate.deactivate(new DeactivateUserCommand(carolId, UUID.randomUUID(), Role.SUPER_ADMIN)));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);

            assertThatThrownBy(() -> b.get(15, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IdentityException.UserAlreadyInactive.class);
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    /** B either queues behind A's lock — what serialising means — or, without one, runs to the end. */
    private void awaitWaitingOrDone(Future<?> b) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10_000;
        while (!b.isDone() && waitingLocks() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    private long waitingLocks() {
        return jdbc.sql("SELECT count(*) FROM pg_locks WHERE NOT granted").query(Long.class).single();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
