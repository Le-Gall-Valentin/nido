package com.synapsys.api.mfa.infrastructure.security;

import com.synapsys.api.mfa.domain.port.out.TotpConfirmAttemptPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisTotpConfirmAttemptStore implements TotpConfirmAttemptPort {

    private static final String KEY_PREFIX = "totp:confirm:attempts:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpConfirmAttemptStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int incrementAndGetAttempts(UUID userId) {
        Long count = redisTemplate.opsForValue().increment(KEY_PREFIX + userId);
        redisTemplate.expire(KEY_PREFIX + userId, TTL);
        return count != null ? count.intValue() : 1;
    }

    @Override
    public void clearAttempts(UUID userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}