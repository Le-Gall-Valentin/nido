package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.application.port.in.RefreshTokenUseCase;
import com.nido.api.authentication.application.port.in.RequestPasswordResetUseCase;
import com.nido.api.authentication.domain.model.UserCredentials;
import com.nido.api.authentication.domain.port.out.RefreshTokenIssuerPort;
import com.nido.api.authentication.domain.port.out.RefreshTokenRevocationPort;
import com.nido.api.authentication.domain.port.out.UserCredentialsPort;
import com.nido.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What must not interleave for one account. Each test holds transaction A open at the worst moment,
 * starts B, waits until B is either waiting on a lock or already done, then lets A commit.
 */
@IntegrationTestConfig
class AccountRacesIT {

    @Autowired RefreshTokenUseCase refresh;
    @Autowired RefreshTokenRevocationPort revocation;
    @Autowired RefreshTokenIssuerPort issuer;
    @Autowired UserCredentialsPort credentialsPort;
    @Autowired RequestPasswordResetUseCase requestReset;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired PasswordResetTokenJpaRepository resetTokens;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcClient jdbc;

    private UUID userId;
    private String username;

    @BeforeEach
    void setUp() {
        UserIdentityEntity user = new UserIdentityEntity();
        username = "race-" + UUID.randomUUID();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        user.setActive(true);
        userId = users.saveAndFlush(user).getId();
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(userId);
        credential.setPasswordHash("$2a$04$unused.for.these.tests.unused.for.these.tests.unused");
        credentials.save(credential);
    }

    @Test
    void a_session_refreshing_while_every_session_is_revoked_does_not_survive_it() throws Exception {
        UserCredentials creds = credentialsPort.findById(userId).orElseThrow();
        String raw = transactions.execute(status -> issuer.generate(creds, 30));
        CountDownLatch aRotated = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // A: a client refreshing its session, caught between the rotation and its commit.
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                refresh.refresh(raw);
                aRotated.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aRotated.await(10, TimeUnit.SECONDS)).isTrue();

            // B: a reset or a password change ending every session at that very moment.
            Future<?> b = pool.submit(() -> revocation.revokeAllForUser(userId));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);

            List<RefreshTokenEntity> tokens = refreshTokens.findAll().stream()
                .filter(token -> token.getUserId().equals(userId)).toList();
            assertThat(tokens).hasSize(2).allMatch(RefreshTokenEntity::isRevoked);
        } finally {
            releaseA.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void two_reset_requests_at_the_same_moment_send_one_link() throws Exception {
        CountDownLatch aIssued = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = pool.submit(() -> transactions.executeWithoutResult(status -> {
                requestReset.request(username);
                aIssued.countDown();
                awaitQuietly(releaseA);
            }));
            assertThat(aIssued.await(10, TimeUnit.SECONDS)).isTrue();

            Future<?> b = pool.submit(() -> requestReset.request(username));
            awaitWaitingOrDone(b);
            releaseA.countDown();
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);

            assertThat(resetTokens.findAll()).filteredOn(token -> token.getUserId().equals(userId)).hasSize(1);
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
