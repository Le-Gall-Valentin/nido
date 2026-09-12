package com.nido.api.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Deletes every rate-limit bucket. Used to give tests a clean slate; nothing in production
     * calls it, and nothing should — but it is reachable, so it scans rather than blocking.
     *
     * <p>KEYS walks the entire keyspace in one go and blocks the single-threaded server for the
     * duration, so on a Redis shared with the challenge store and the anti-replay set it stalls
     * every login too. SCAN returns in bounded batches instead, at the cost of being approximate
     * under concurrent writes — which is exactly the right trade for a bulk cleanup.
     */
    public void clearAll() {
        ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(500).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            List<String> batch = new ArrayList<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 500) {
                    redisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                redisTemplate.delete(batch);
            }
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