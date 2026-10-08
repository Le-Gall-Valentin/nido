package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.model.CodePurpose;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTotpAttemptStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private RedisTotpAttemptStore store;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        store = new RedisTotpAttemptStore(redisTemplate);
    }

    @Test
    void a_wrong_first_code_is_counted_for_an_hour_under_the_key_it_always_had() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(3L);

        assertThat(store.recordFailure(userId, CodePurpose.ENROL)).isEqualTo(3);
        verify(redisTemplate).expire("totp:confirm:attempts:" + userId, Duration.ofHours(1));
    }

    @Test
    void a_wrong_code_to_turn_the_app_off_is_counted_for_a_quarter_of_an_hour_apart() {
        when(valueOps.increment("totp:disable:attempts:" + userId)).thenReturn(1L);

        assertThat(store.recordFailure(userId, CodePurpose.DISABLE)).isEqualTo(1);
        verify(redisTemplate).expire("totp:disable:attempts:" + userId, Duration.ofMinutes(15));
    }

    @Test
    void a_missing_answer_from_redis_counts_as_the_first() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(null);

        assertThat(store.recordFailure(userId, CodePurpose.ENROL)).isEqualTo(1);
    }

    @Test
    void the_failures_are_read_without_counting_one() {
        when(valueOps.get("totp:disable:attempts:" + userId)).thenReturn("5", (String) null);

        assertThat(store.failures(userId, CodePurpose.DISABLE)).isEqualTo(5);
        assertThat(store.failures(userId, CodePurpose.DISABLE)).isZero();
        verify(valueOps, never()).increment(anyString());
    }

    @Test
    void clearing_drops_the_count_of_that_purpose_only() {
        store.clear(userId, CodePurpose.DISABLE);

        verify(redisTemplate).delete("totp:disable:attempts:" + userId);
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void sign_in_is_counted_by_the_account_not_here() {
        assertThatThrownBy(() -> store.recordFailure(userId, CodePurpose.LOGIN)).isInstanceOf(IllegalArgumentException.class);
    }
}
