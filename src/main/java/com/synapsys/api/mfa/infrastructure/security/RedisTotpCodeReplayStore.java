package com.synapsys.api.mfa.infrastructure.security;

import com.synapsys.api.mfa.domain.port.out.TotpCodeReplayPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.UUID;

@Component
public class RedisTotpCodeReplayStore implements TotpCodeReplayPort {

    private static final String USED_CODE_PREFIX = "totp:used:";
    private static final Duration ANTI_REPLAY_TTL = Duration.ofSeconds(150);

    private final StringRedisTemplate redisTemplate;

    public RedisTotpCodeReplayStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean markCodeUsedIfAbsent(UUID userId, String code) {
        Boolean set = redisTemplate.opsForValue()
            .setIfAbsent(USED_CODE_PREFIX + userId + ":" + code, "1", ANTI_REPLAY_TTL);
        return Boolean.TRUE.equals(set);
    }
}