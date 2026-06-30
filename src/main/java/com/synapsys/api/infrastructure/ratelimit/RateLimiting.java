package com.synapsys.api.infrastructure.ratelimit;

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
 * USER mode requires an authenticated Spring Security principal.
 * On unauthenticated endpoints the USER rule is silently skipped.
 */
@Repeatable(RateLimitingList.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiting {
    RateLimitMode mode()   default RateLimitMode.IP;
    int max()              default 10;
    int windowSeconds()    default 60;
}