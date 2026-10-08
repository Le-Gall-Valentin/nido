package com.nido.api.mfa.infrastructure.security;

import com.nido.api.mfa.domain.model.CodePurpose;
import com.nido.api.mfa.domain.port.out.TotpAttemptPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Each count lives in its own key, renewed by every failure. An enrolment's count keeps the key and the hour it
 * always had. Turning the app off waits a quarter of an hour once spent: refused attempts are not counted, so
 * that quarter runs from the fifth wrong code.
 */
@Component
public class RedisTotpAttemptStore implements TotpAttemptPort {

    private static final String ENROL_PREFIX = "totp:confirm:attempts:";
    private static final Duration ENROL_TTL = Duration.ofHours(1);
    private static final String DISABLE_PREFIX = "totp:disable:attempts:";
    private static final Duration DISABLE_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpAttemptStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int recordFailure(UUID userId, CodePurpose purpose) {
        String key = keyOf(userId, purpose);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ttlOf(purpose));
        return count != null ? count.intValue() : 1;
    }

    @Override
    public int failures(UUID userId, CodePurpose purpose) {
        String count = redisTemplate.opsForValue().get(keyOf(userId, purpose));
        return count != null ? Integer.parseInt(count) : 0;
    }

    @Override
    public void clear(UUID userId, CodePurpose purpose) {
        redisTemplate.delete(keyOf(userId, purpose));
    }

    private static String keyOf(UUID userId, CodePurpose purpose) {
        return switch (purpose) {
            case ENROL -> ENROL_PREFIX + userId;
            case DISABLE -> DISABLE_PREFIX + userId;
            case LOGIN, EMAIL_CHANGE -> throw new IllegalArgumentException("The app counts no wrong code for " + purpose);
        };
    }

    private static Duration ttlOf(CodePurpose purpose) {
        return purpose == CodePurpose.ENROL ? ENROL_TTL : DISABLE_TTL;
    }
}
