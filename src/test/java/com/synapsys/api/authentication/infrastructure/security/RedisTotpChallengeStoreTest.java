package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.infrastructure.config.SynapsysProperties;
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
        var properties = new SynapsysProperties(null, null, null, null, null, null, null,
            new SynapsysProperties.SecurityProperties(20));
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
    void invalidateChallenge_deletesChallengeAndAttemptsKeys() {
        String challengeId = UUID.randomUUID().toString();

        store.invalidateChallenge(challengeId);

        verify(redisTemplate).delete("totp:challenge:" + challengeId);
        verify(redisTemplate).delete("totp:attempts:" + challengeId);
    }

    @Test
    void incrementFailedAttempts_firstAttempt_returns1_andSetsTtl() {
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.increment("totp:attempts:" + challengeId)).thenReturn(1L);

        int count = store.incrementFailedAttempts(challengeId);

        assertThat(count).isEqualTo(1);
        verify(redisTemplate).expire(
            eq("totp:attempts:" + challengeId),
            eq(Duration.ofMinutes(20))
        );
    }

    @Test
    void incrementFailedAttempts_subsequentAttempts_alsoSetsTtl() {
        // expire() is called unconditionally to avoid the non-atomic window where
        // a crash between increment and expire would leave a key without TTL.
        String challengeId = UUID.randomUUID().toString();
        when(valueOps.increment("totp:attempts:" + challengeId)).thenReturn(3L);

        int count = store.incrementFailedAttempts(challengeId);

        assertThat(count).isEqualTo(3);
        verify(redisTemplate).expire(eq("totp:attempts:" + challengeId), eq(Duration.ofMinutes(20)));
    }
}