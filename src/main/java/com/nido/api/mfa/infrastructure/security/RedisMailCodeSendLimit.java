package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.port.out.MailCodeSendLimitPort;
import com.nido.api.shared.model.TwoFactorPolicy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
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
    /** Takes one back unless the window has closed meanwhile: a DECR would revive the key with no expiry. */
    private static final RedisScript<Long> GIVE_BACK = RedisScript.of(
        "if redis.call('EXISTS', KEYS[1]) == 1 then return redis.call('DECR', KEYS[1]) end return 0", Long.class);

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
        if (sends > TwoFactorPolicy.MAIL_SENDS_PER_WINDOW) {
            return OptionalLong.of(Math.max(1L, ttl));
        }
        giveBackIfRolledBack(key);
        return OptionalLong.empty();
    }

    /** The mail is queued in the caller's transaction: rolled back, it never leaves, and must not use up the window. */
    private void giveBackIfRolledBack(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    redis.execute(GIVE_BACK, List.of(key));
                }
            }
        });
    }
}
