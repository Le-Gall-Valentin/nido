package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.domain.model.PasswordResetToken;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class PasswordResetTokenRepositoryIT {

    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetTokenJpaRepository jpa;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired TransactionTemplate transactions;

    private final Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
    private UUID userId;

    @BeforeEach
    void setUp() {
        jpa.deleteAll();
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("reset-" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@test.com");
        user.setRole(Role.USER);
        userId = users.saveAndFlush(user).getId();
    }

    @Test
    void a_token_is_found_by_its_hash() {
        tokens.save(userId, "a".repeat(64), now, now.plus(Duration.ofMinutes(30)));

        assertThat(tokens.findByHash("a".repeat(64))).hasValueSatisfying(token -> {
            assertThat(token.userId()).isEqualTo(userId);
            assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(30)));
        });
        assertThat(tokens.findByHash("b".repeat(64))).isEmpty();
    }

    @Test
    void consuming_twice_gives_the_token_once() {
        tokens.save(userId, "a".repeat(64), now, now.plus(Duration.ofMinutes(30)));

        Optional<PasswordResetToken> first = transactions.execute(status -> tokens.consumeByHash("a".repeat(64)));
        Optional<PasswordResetToken> second = transactions.execute(status -> tokens.consumeByHash("a".repeat(64)));

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        assertThat(jpa.count()).isZero();
    }

    @Test
    void a_consumed_token_stays_deleted_when_a_clearing_update_follows_in_the_same_transaction() {
        tokens.save(userId, "a".repeat(64), now, now.plus(Duration.ofMinutes(30)));

        // What the confirmation does: updatePasswordHash clears the persistence context without flushing
        // first, so the delete must already have gone out — nothing else here deletes the row.
        transactions.executeWithoutResult(status -> {
            tokens.consumeByHash("a".repeat(64));
            credentials.updatePasswordHash(userId, "new-hash");
        });

        assertThat(jpa.count()).isZero();
    }

    @Test
    void two_confirmations_racing_with_one_link_get_it_once() throws Exception {
        String hash = "a".repeat(64);
        tokens.save(userId, hash, now, now.plus(Duration.ofMinutes(30)));
        CountDownLatch aHasConsumed = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Optional<PasswordResetToken>> a = pool.submit(() -> transactions.execute(status -> {
                Optional<PasswordResetToken> result = tokens.consumeByHash(hash);
                aHasConsumed.countDown();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return result;
            }));
            Future<Optional<PasswordResetToken>> b = pool.submit(() -> {
                assertThat(aHasConsumed.await(10, TimeUnit.SECONDS)).isTrue();
                return transactions.execute(status -> tokens.consumeByHash(hash));
            });

            List<Optional<PasswordResetToken>> results = List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));

            assertThat(results.stream().filter(Optional::isPresent).count()).isEqualTo(1);
            assertThat(jpa.count()).isZero();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void remembers_when_the_last_link_went_out() {
        assertThat(tokens.latestIssuedAt(userId)).isEmpty();

        tokens.save(userId, "a".repeat(64), now.minusSeconds(120), now.plusSeconds(1680));
        tokens.save(userId, "b".repeat(64), now, now.plusSeconds(1800));

        assertThat(tokens.latestIssuedAt(userId)).contains(now);
    }

    @Test
    void deletes_every_token_of_an_account() {
        tokens.save(userId, "a".repeat(64), now, now.plusSeconds(1800));
        tokens.save(userId, "b".repeat(64), now, now.plusSeconds(1800));

        transactions.executeWithoutResult(status -> tokens.deleteAllForUser(userId));

        assertThat(jpa.count()).isZero();
    }

    @Test
    void the_purge_removes_expired_tokens_only() {
        tokens.save(userId, "a".repeat(64), now.minusSeconds(3600), now.minusSeconds(1));
        tokens.save(userId, "b".repeat(64), now, now.plusSeconds(1800));

        assertThat(tokens.deleteExpired(now)).isEqualTo(1);
        assertThat(tokens.findByHash("b".repeat(64))).isPresent();
    }

    @Test
    void an_account_removed_from_the_table_takes_its_tokens_with_it() {
        tokens.save(userId, "a".repeat(64), now, now.plusSeconds(1800));

        users.deleteById(userId);
        users.flush();

        assertThat(jpa.count()).isZero();
    }
}
