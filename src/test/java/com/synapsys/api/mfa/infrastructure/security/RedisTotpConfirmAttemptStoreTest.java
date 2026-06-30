package com.synapsys.api.mfa.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTotpConfirmAttemptStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private RedisTotpConfirmAttemptStore store;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisTotpConfirmAttemptStore(redisTemplate);
    }

    @Test
    void incrementAndGetAttempts_firstCall_returns1_andSetsTtl() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(1L);

        int result = store.incrementAndGetAttempts(userId);

        assertThat(result).isEqualTo(1);
        verify(redisTemplate).expire(
            eq("totp:confirm:attempts:" + userId),
            eq(Duration.ofHours(1))
        );
    }

    @Test
    void incrementAndGetAttempts_subsequentCalls_alsoSetsTtl() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(3L);

        int result = store.incrementAndGetAttempts(userId);

        assertThat(result).isEqualTo(3);
        verify(redisTemplate).expire(eq("totp:confirm:attempts:" + userId), eq(Duration.ofHours(1)));
    }

    @Test
    void incrementAndGetAttempts_nullResult_returns1() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(null);

        int result = store.incrementAndGetAttempts(userId);

        assertThat(result).isEqualTo(1);
    }

    @Test
    void clearAttempts_deletesKey() {
        store.clearAttempts(userId);

        verify(redisTemplate).delete("totp:confirm:attempts:" + userId);
    }
}