package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TotpAttemptPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Each count lives in its own key, in a fixed window opened by its first INCR. An enrolment's count keeps the key
 * and the hour it always had — longer than an enrolment lives. Turning the app off is counted for a quarter of an
 * hour, refused tries included, which never push the end of the wait.
 */
@Component
public class RedisTotpAttemptStore implements TotpAttemptPort {

    private static final String ENROL_PREFIX = "totp:confirm:attempts:";
    private static final Duration ENROL_WINDOW = Duration.ofHours(1);
    private static final String DISABLE_PREFIX = "totp:disable:attempts:";
    private static final Duration DISABLE_WINDOW = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpAttemptStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int record(UUID userId, CodePurpose purpose) {
        String key = keyOf(userId, purpose);
        Long count = redisTemplate.opsForValue().increment(key);
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        // The first one opens the window. A key without expiry — a crash between INCR and EXPIRE — would
        // otherwise count for good, so it gets one back too.
        if (count == null || count == 1L || ttl == null || ttl < 0) {
            redisTemplate.expire(key, windowOf(purpose));
        }
        return count != null ? count.intValue() : 1;
    }

    @Override
    public void clear(UUID userId, CodePurpose purpose) {
        redisTemplate.delete(keyOf(userId, purpose));
    }

    private static String keyOf(UUID userId, CodePurpose purpose) {
        return switch (purpose) {
            case ENROL -> ENROL_PREFIX + userId;
            case DISABLE -> DISABLE_PREFIX + userId;
            case LOGIN, EMAIL_CHANGE -> throw new IllegalArgumentException("The app counts no code for " + purpose);
        };
    }

    private static Duration windowOf(CodePurpose purpose) {
        return purpose == CodePurpose.ENROL ? ENROL_WINDOW : DISABLE_WINDOW;
    }
}
