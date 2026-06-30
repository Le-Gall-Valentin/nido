package com.synapsys.api.infrastructure.ratelimit;

public class RateLimitExceededException extends RuntimeException {

    private final long limit;
    private final long remaining;
    private final long resetEpochSeconds;
    private final long retryAfterSeconds;

    RateLimitExceededException(RateLimitHeaders headers) {
        super("Too many requests. Please try again later.");
        this.limit = headers.limit();
        this.remaining = Math.max(0L, headers.remaining());
        this.resetEpochSeconds = headers.resetEpochSeconds();
        this.retryAfterSeconds = Math.max(1L, headers.retryAfterSeconds());
    }

    public long getLimit()             { return limit; }
    public long getRemaining()         { return remaining; }
    public long getResetEpochSeconds() { return resetEpochSeconds; }
    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}