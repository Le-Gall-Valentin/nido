package com.synapsys.api.mfa.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisTotpCodeReplayStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks RedisTotpCodeReplayStore store;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void markCodeUsedIfAbsent_firstCall_returnsTrue() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);

        assertThat(store.markCodeUsedIfAbsent(userId, "123456")).isTrue();
    }

    @Test
    void markCodeUsedIfAbsent_codeAlreadyUsed_returnsFalse() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        assertThat(store.markCodeUsedIfAbsent(userId, "123456")).isFalse();
    }

    @Test
    void markCodeUsedIfAbsent_nullFromRedis_returnsFalse() {
        when(valueOps.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(null);

        assertThat(store.markCodeUsedIfAbsent(userId, "123456")).isFalse();
    }

    @Test
    void markCodeUsedIfAbsent_keyContainsUserIdAndCode() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        store.markCodeUsedIfAbsent(userId, "654321");

        verify(valueOps).setIfAbsent(
            argThat(key -> key.contains(userId.toString()) && key.contains("654321")),
            eq("1"),
            any(Duration.class)
        );
    }
}