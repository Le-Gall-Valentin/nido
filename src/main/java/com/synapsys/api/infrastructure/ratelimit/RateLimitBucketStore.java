package com.synapsys.api.infrastructure.ratelimit;

/**
 * Port for token-bucket storage. Swap implementations to migrate from Caffeine to Redis
 * without touching the annotation, interceptor or exception handler.
 */
public interface RateLimitBucketStore {

    /**
     * @param key           composite key: "ClassName.method:MODE:identifier"
     * @param max           bucket capacity (= max tokens)
     * @param windowSeconds full-refill period in seconds
     * @return result of attempting to consume 1 token
     */
    BucketResult tryConsume(String key, int max, int windowSeconds);

    /**
     * Non-consuming check: returns whether 1 token could be consumed without actually consuming it.
     * Used for pre-flight checks before committing consumption across multiple rules.
     */
    BucketResult peekConsume(String key, int max, int windowSeconds);

    record BucketResult(boolean allowed, long remaining, long nanosToWaitForRefill) {}
}