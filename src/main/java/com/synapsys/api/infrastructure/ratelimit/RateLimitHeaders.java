package com.synapsys.api.infrastructure.ratelimit;

/**
 * Aggregates rate-limit header values across multiple bucket checks.
 * "Worst-case" merging: lowest remaining, latest reset, longest retry-after.
 */
record RateLimitHeaders(long limit, long remaining, long resetEpochSeconds, long retryAfterSeconds) {

    static RateLimitHeaders from(RateLimitBucketStore.BucketResult result, int max, long nowSeconds) {
        long retryAfterSeconds = result.nanosToWaitForRefill() <= 0 ? 0L
            : Math.max(1L, (result.nanosToWaitForRefill() + 999_999_999L) / 1_000_000_000L);
        long resetEpochSeconds = nowSeconds + retryAfterSeconds;
        return new RateLimitHeaders(max, Math.max(0L, result.remaining()), resetEpochSeconds, retryAfterSeconds);
    }

    RateLimitHeaders worstCase(RateLimitHeaders other) {
        return new RateLimitHeaders(
            Math.min(this.limit, other.limit),
            Math.min(this.remaining, other.remaining),
            Math.max(this.resetEpochSeconds, other.resetEpochSeconds),
            Math.max(this.retryAfterSeconds, other.retryAfterSeconds)
        );
    }
}