package com.nido.api.authentication.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.domain.model.PasswordResetToken;
import com.nido.api.authentication.domain.port.out.PasswordResetTokenRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class PasswordResetTokenRepositoryIT {

    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetTokenJpaRepository jpa;
    @Autowired UserIdentityJpaRepository users;
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
