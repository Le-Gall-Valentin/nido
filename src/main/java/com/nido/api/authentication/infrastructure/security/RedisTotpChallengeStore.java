package com.nido.api.authentication.infrastructure.security;

import com.nido.api.authentication.domain.port.out.TotpChallengeStorePort;
import com.nido.api.infrastructure.config.NidoProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisTotpChallengeStore implements TotpChallengeStorePort {

    private static final String CHALLENGE_PREFIX = "totp:challenge:";
    private static final String ATTEMPTS_PREFIX = "totp:attempts:user:";

    private static final int DEFAULT_CHALLENGE_TTL_MINUTES = 15;
    private static final int DEFAULT_LOCKOUT_MINUTES = 15;

    private final StringRedisTemplate redisTemplate;
    private final Duration challengeTtl;
    private final Duration lockoutWindow;

    public RedisTotpChallengeStore(StringRedisTemplate redisTemplate, NidoProperties properties) {
        this.redisTemplate = redisTemplate;
        NidoProperties.SecurityProperties security = properties.security();
        this.challengeTtl = Duration.ofMinutes(
            security != null ? security.challengeTtlMinutes() : DEFAULT_CHALLENGE_TTL_MINUTES);
        this.lockoutWindow = Duration.ofMinutes(
            security != null ? security.totpLockoutMinutes() : DEFAULT_LOCKOUT_MINUTES);
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
        // Only the challenge: the attempt counter outlives it on purpose, since a fresh
        // challenge is exactly what a caller obtains by logging in again.
        redisTemplate.delete(CHALLENGE_PREFIX + challengeId);
    }

    @Override
    public int failedAttempts(UUID userId) {
        String value = redisTemplate.opsForValue().get(ATTEMPTS_PREFIX + userId);
        if (value == null) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // A key holding something other than a number can only be corruption or a
            // collision with another writer. Reading it as "locked out" would deny the account
            // its own logins, so the safe reading is "no attempts recorded" — the counter then
            // rebuilds itself from the next failure.
            return 0;
        }
    }

    @Override
    public int recordFailedAttempt(UUID userId) {
        String key = ATTEMPTS_PREFIX + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        long attempts = count != null ? count : 1L;
        // expire() is called unconditionally, as it was when this counter hung off the
        // challenge: INCR creates the key without a TTL, so a crash between the two would
        // leave it there forever — and now that the key is the account's, forever would mean
        // a permanently unusable account rather than a stale challenge.
        //
        // The window therefore slides while attempts are still being recorded, and stops
        // sliding the moment the account is locked out: callers refuse a locked-out attempt
        // before reaching this method, so nothing renews the TTL. The lockout runs for the
        // configured span from the attempt that exhausted it, and no longer — a caller who
        // keeps hammering cannot extend it.
        redisTemplate.expire(key, lockoutWindow);
        return (int) attempts;
    }

    @Override
    public void clearFailedAttempts(UUID userId) {
        redisTemplate.delete(ATTEMPTS_PREFIX + userId);
    }
}
