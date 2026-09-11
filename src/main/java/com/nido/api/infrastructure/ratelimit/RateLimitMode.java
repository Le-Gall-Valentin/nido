package com.nido.api.infrastructure.ratelimit;

public enum RateLimitMode {
    /**
     * Limit by account when the caller is authenticated, and fall back to the IP when they are not.
     * The default, and what almost every endpoint wants.
     *
     * <p>On an authenticated endpoint the account is the unit that matters: the caller had to have
     * one to get in, so counting per account is both precise and fair. Counting per IP there
     * charges a household, an office or a phone network as a single caller — everyone behind one
     * NAT shares a bucket and locks each other out, while an attacker with one account simply
     * switches IP. Counting per account inverts both.
     *
     * <p>Falling back to the IP is what makes this safe as a default: on a public endpoint there is
     * no account to count, and {@link #USER} would silently mean no limit at all. Here the IP still
     * answers for it.
     */
    USER_ELSE_IP,
    /** Rate limit by client IP address only. */
    IP,
    /** Rate limit by authenticated user (Spring Security principal) only.
     *  Silently ignored on unauthenticated endpoints. */
    USER,
    /** Rate limit by both IP and user independently — a single exceeded bucket blocks the request. */
    IP_AND_USER
}