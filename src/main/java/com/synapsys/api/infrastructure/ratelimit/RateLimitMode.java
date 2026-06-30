package com.synapsys.api.infrastructure.ratelimit;

public enum RateLimitMode {
    /** Rate limit by client IP address only. */
    IP,
    /** Rate limit by authenticated user (Spring Security principal) only.
     *  Silently ignored on unauthenticated endpoints. */
    USER,
    /** Rate limit by both IP and user independently — a single exceeded bucket blocks the request. */
    IP_AND_USER
}