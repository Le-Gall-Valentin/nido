package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.port.out.MailCodeSendLimitPort;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A fixed window per account, opened by its first mail. Under the totp: prefix like every other key of
 * mfa: the production ACL lets the application touch nothing else.
 */
@Component
public class RedisMailCodeSendLimit implements MailCodeSendLimitPort {

    private static final String PREFIX = "totp:mail-sends:user:";

    private final StringRedisTemplate redis;

    public RedisMailCodeSendLimit(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public OptionalLong tryCount(UUID userId) {
        String key = PREFIX + userId;
        Long count = redis.opsForValue().increment(key);
        long sends = count != null ? count : 1L;
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        // The first mail opens the window. A key without expiry — a crash between INCR and EXPIRE — would
        // otherwise refuse this account's mails for good, so it gets one back too.
        if (sends == 1L || ttl == null || ttl < 0) {
            redis.expire(key, TwoFactorPolicy.MAIL_SEND_WINDOW);
            ttl = TwoFactorPolicy.MAIL_SEND_WINDOW.toSeconds();
        }
        return sends > TwoFactorPolicy.MAIL_SENDS_PER_WINDOW ? OptionalLong.of(Math.max(1L, ttl)) : OptionalLong.empty();
    }
}
