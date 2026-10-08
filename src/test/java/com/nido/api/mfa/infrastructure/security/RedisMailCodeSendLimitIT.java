package com.nido.api.mfa.infrastructure.security;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.domain.port.out.MailCodeSendLimitPort;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class RedisMailCodeSendLimitIT {

    @Autowired MailCodeSendLimitPort limit;
    @Autowired StringRedisTemplate redis;

    private final UUID account = UUID.randomUUID();

    @BeforeEach
    void clean() {
        redis.delete("totp:mail-sends:user:" + account);
    }

    @Test
    void five_mails_leave_and_the_sixth_waits_for_the_window() {
        for (int i = 0; i < TwoFactorPolicy.MAIL_SENDS_PER_WINDOW; i++) {
            assertThat(limit.tryCount(account)).as("send %d", i + 1).isEmpty();
        }

        var refused = limit.tryCount(account);

        assertThat(refused).isPresent();
        assertThat(refused.getAsLong()).isBetween(1L, TwoFactorPolicy.MAIL_SEND_WINDOW.toSeconds());
    }

    @Test
    void the_window_is_set_by_the_first_mail_and_lives_under_the_totp_prefix() {
        limit.tryCount(account);

        Long ttl = redis.getExpire("totp:mail-sends:user:" + account, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(1L, TwoFactorPolicy.MAIL_SEND_WINDOW.toSeconds());
    }

    @Test
    void a_key_left_without_expiry_by_a_crash_gets_one_back() {
        redis.opsForValue().set("totp:mail-sends:user:" + account, "2");

        limit.tryCount(account);

        assertThat(redis.getExpire("totp:mail-sends:user:" + account, TimeUnit.SECONDS)).isPositive();
    }
}
