package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.port.out.TotpChallengeStorePort;
import com.synapsys.api.infrastructure.config.SynapsysProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisTotpChallengeStore implements TotpChallengeStorePort {

    private static final String CHALLENGE_PREFIX = "totp:challenge:";

    private final StringRedisTemplate redisTemplate;
    private final Duration challengeTtl;

    public RedisTotpChallengeStore(StringRedisTemplate redisTemplate, SynapsysProperties properties) {
        this.redisTemplate = redisTemplate;
        int ttlMinutes = properties.security() != null ? properties.security().challengeTtlMinutes() : 15;
        this.challengeTtl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public String createChallenge(UUID userId) {
        String challengeId = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CHALLENGE_PREFIX + challengeId, userId.toString(), challengeTtl);
        return challengeId;
    }

    @Override
    public Optional<UUID> resolveChallenge(String challengeId) {
        String value = redisTemplate.opsForValue().get(CHALLENGE_PREFIX + challengeId);
        if (value == null) return Optional.empty();
        return Optional.of(UUID.fromString(value));
    }

    @Override
    public void invalidateChallenge(String challengeId) {
        redisTemplate.delete(CHALLENGE_PREFIX + challengeId);
        redisTemplate.delete("totp:attempts:" + challengeId);
    }

    @Override
    public int incrementFailedAttempts(String challengeId) {
        String key = "totp:attempts:" + challengeId;
        Long count = redisTemplate.opsForValue().increment(key);
        long attempts = count != null ? count : 1L;
        // expire() is called unconditionally to avoid the non-atomic increment+expire window:
        // if the process crashes after increment but before expire, the key would leak forever.
        redisTemplate.expire(key, challengeTtl);
        return (int) attempts;
    }
}