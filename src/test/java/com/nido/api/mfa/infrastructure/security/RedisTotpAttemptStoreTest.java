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
import java.util.concurrent.TimeUnit;

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
    void the_first_wrong_first_code_opens_an_hour_under_the_key_it_always_had() {
        when(valueOps.increment("totp:confirm:attempts:" + userId)).thenReturn(1L);
        when(redisTemplate.getExpire("totp:confirm:attempts:" + userId, TimeUnit.SECONDS)).thenReturn(-1L);

        assertThat(store.record(userId, CodePurpose.ENROL)).isEqualTo(1);
        verify(redisTemplate).expire("totp:confirm:attempts:" + userId, Duration.ofHours(1));
    }

    @Test
    void the_first_try_to_turn_the_app_off_opens_a_quarter_of_an_hour_apart() {
        when(valueOps.increment("totp:disable:attempts:" + userId)).thenReturn(1L);
        when(redisTemplate.getExpire("totp:disable:attempts:" + userId, TimeUnit.SECONDS)).thenReturn(-1L);

        assertThat(store.record(userId, CodePurpose.DISABLE)).isEqualTo(1);
        verify(redisTemplate).expire("totp:disable:attempts:" + userId, Duration.ofMinutes(15));
    }

    @Test
    void later_tries_leave_the_window_where_the_first_opened_it() {
        // Renewed by every try, refused ones included, the wait would never end for someone still trying.
        when(valueOps.increment("totp:disable:attempts:" + userId)).thenReturn(7L);
        when(redisTemplate.getExpire("totp:disable:attempts:" + userId, TimeUnit.SECONDS)).thenReturn(300L);

        assertThat(store.record(userId, CodePurpose.DISABLE)).isEqualTo(7);
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void a_count_left_without_expiry_gets_one_back() {
        // A crash between INCR and EXPIRE would otherwise hold the app on for good.
        when(valueOps.increment("totp:disable:attempts:" + userId)).thenReturn(3L);
        when(redisTemplate.getExpire("totp:disable:attempts:" + userId, TimeUnit.SECONDS)).thenReturn(-1L);

        store.record(userId, CodePurpose.DISABLE);

        verify(redisTemplate).expire("totp:disable:attempts:" + userId, Duration.ofMinutes(15));
    }

    @Test
    void clearing_drops_the_count_of_that_purpose_only() {
        store.clear(userId, CodePurpose.DISABLE);

        verify(redisTemplate).delete("totp:disable:attempts:" + userId);
        verifyNoMoreInteractions(redisTemplate);
    }

    @Test
    void sign_in_is_counted_by_the_account_not_here() {
        assertThatThrownBy(() -> store.record(userId, CodePurpose.LOGIN)).isInstanceOf(IllegalArgumentException.class);
    }
}
