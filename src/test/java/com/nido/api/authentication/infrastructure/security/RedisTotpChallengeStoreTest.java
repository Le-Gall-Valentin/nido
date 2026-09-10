package com.nido.api.authentication.infrastructure.security;

import com.nido.api.infrastructure.config.NidoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTotpChallengeStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private RedisTotpChallengeStore store;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        var properties = new NidoProperties(null, null, null, null, null, null, null,
            new NidoProperties.SecurityProperties(20, 30));
        store = new RedisTotpChallengeStore(redisTemplate, properties);
    }

    @Test
    void createChallenge_storesUserIdWithConfiguredTtl_andReturnsUuid() {
        String challengeId = store.createChallenge(userId);

        assertThat(challengeId).isNotBlank();
        assertThat(UUID.fromString(challengeId)).isNotNull();
        verify(valueOps).set(
            eq("totp:challenge:" + challengeId),
            eq(userId.toString()),
            eq(Duration.ofMinutes(20))
        );
    }

    @Test
    void resolveChallenge_existingKey_returnsUserId() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.get("totp:challenge:" + challengeId)).thenReturn(userId.toString());

        Optional<UUID> result = store.resolveChallenge(challengeId);

        assertThat(result).contains(userId);
    }

    @Test
    void resolveChallenge_missingKey_returnsEmpty() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.get("totp:challenge:" + challengeId)).thenReturn(null);

        Optional<UUID> result = store.resolveChallenge(challengeId);

        assertThat(result).isEmpty();
    }

    @Test
    void invalidateChallenge_deletesTheChallengeButLeavesTheAccountCounterStanding() {
        // The counter has to outlive the challenge: obtaining a fresh challenge is precisely
        // what logging in again does, and that must not hand back a clean slate.
        String challengeId = UUID.randomUUID().toString();

        store.invalidateChallenge(challengeId);

        verify(redisTemplate).delete("totp:challenge:" + challengeId);
        verify(redisTemplate, never()).delete(startsWith("totp:attempts:"));
    }

    @Test
    void recordFailedAttempt_isKeyedOnTheAccount_andArmsTheLockoutWindow() {
        when(valueOps.increment("totp:attempts:user:" + userId)).thenReturn(1L);

        int count = store.recordFailedAttempt(userId);

        assertThat(count).isEqualTo(1);
        // The lockout window, not the challenge TTL — two different spans, 30 and 20 here.
        verify(redisTemplate).expire(eq("totp:attempts:user:" + userId), eq(Duration.ofMinutes(30)));
    }

    @Test
    void recordFailedAttempt_setsTheTtlOnEveryAttempt() {
        // INCR creates the key without a TTL, so a crash between increment and expire would
        // leave the account permanently locked. Cheaper to re-set it every time than to risk that.
        when(valueOps.increment("totp:attempts:user:" + userId)).thenReturn(3L);

        int count = store.recordFailedAttempt(userId);

        assertThat(count).isEqualTo(3);
        verify(redisTemplate).expire(eq("totp:attempts:user:" + userId), eq(Duration.ofMinutes(30)));
    }

    @Test
    void failedAttempts_readsTheAccountCounter() {
        when(valueOps.get("totp:attempts:user:" + userId)).thenReturn("4");

        assertThat(store.failedAttempts(userId)).isEqualTo(4);
    }

    @Test
    void failedAttempts_isZeroWhenNothingWasRecorded() {
        when(valueOps.get("totp:attempts:user:" + userId)).thenReturn(null);

        assertThat(store.failedAttempts(userId)).isZero();
    }

    @Test
    void failedAttempts_readsAnUnparseableValueAsZeroRatherThanLockingTheAccountOut() {
        // Corruption or a key collision must not be read as "locked out": that would deny the
        // account its own logins with no way back until the key expires.
        when(valueOps.get("totp:attempts:user:" + userId)).thenReturn("not-a-number");

        assertThat(store.failedAttempts(userId)).isZero();
    }

    @Test
    void clearFailedAttempts_removesTheAccountCounter() {
        store.clearFailedAttempts(userId);

        verify(redisTemplate).delete("totp:attempts:user:" + userId);
    }
}