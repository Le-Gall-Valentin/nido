package com.nido.api.infrastructure.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative token-bucket rate limiting for controller endpoints.
 * Repeatable: stack multiple @RateLimiting on the same method for combined limits.
 *
 * <pre>{@code
 * @PostMapping("/login")
 * @RateLimiting(mode = RateLimitMode.IP,   max = 10, windowSeconds = 60)
 * @RateLimiting(mode = RateLimitMode.USER, max = 5,  windowSeconds = 300)
 * public ResponseEntity<?> login(...) { ... }
 * }</pre>
 *
 * <p>The default is {@link RateLimitMode#USER_ELSE_IP}: count per account where there is one, per
 * IP where there is not. Reach for another mode only with a reason — {@link RateLimitMode#IP} on an
 * authenticated endpoint charges everybody behind one NAT as a single caller, and
 * {@link RateLimitMode#USER} on a public one is silently no limit at all.
 */
@Repeatable(RateLimitingList.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiting {
    RateLimitMode mode()   default RateLimitMode.USER_ELSE_IP;
    int max()              default 10;
    int windowSeconds()    default 60;
}