package com.synapsys.api.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisRateLimitBucketStore implements RateLimitBucketStore {

    static final String KEY_PREFIX = "ratelimit:";

    private final ProxyManager<String> proxyManager;
    private final StringRedisTemplate redisTemplate;

    RedisRateLimitBucketStore(ProxyManager<String> proxyManager, StringRedisTemplate redisTemplate) {
        this.proxyManager = proxyManager;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public BucketResult tryConsume(String key, int max, int windowSeconds) {
        ConsumptionProbe probe = proxyManager.builder()
            .build(KEY_PREFIX + key, () -> config(max, windowSeconds))
            .tryConsumeAndReturnRemaining(1);
        return new BucketResult(probe.isConsumed(), probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
    }

    @Override
    public BucketResult peekConsume(String key, int max, int windowSeconds) {
        EstimationProbe probe = proxyManager.builder()
            .build(KEY_PREFIX + key, () -> config(max, windowSeconds))
            .estimateAbilityToConsume(1);
        return new BucketResult(probe.canBeConsumed(), probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
    }

    public void clearAll() {
        var keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private static BucketConfiguration config(int max, int windowSeconds) {
        return BucketConfiguration.builder()
            .addLimit(Bandwidth.builder()
                .capacity(max)
                .refillGreedy(max, Duration.ofSeconds(windowSeconds))
                .build())
            .build();
    }
}