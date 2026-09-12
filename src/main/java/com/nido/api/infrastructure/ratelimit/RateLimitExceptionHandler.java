package com.nido.api.infrastructure.ratelimit;

import com.nido.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

// Ahead of GlobalExceptionHandler's last resort, which matches Exception and would otherwise be
// picked first on an order tie — turning a declared domain error into a 500.
@Order(Ordered.LOWEST_PRECEDENCE - 100)
@RestControllerAdvice
public class RateLimitExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handle(RateLimitExceededException e, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetailFactory.of(
            HttpStatus.TOO_MANY_REQUESTS, "RateLimitExceeded",
            "Too many requests. Please try again later.",
            URI.create(request.getRequestURI()));
        return ResponseEntity.status(429)
            .header("X-RateLimit-Limit",     String.valueOf(e.getLimit()))
            .header("X-RateLimit-Remaining", "0")
            .header("X-RateLimit-Reset",     String.valueOf(e.getResetEpochSeconds()))
            .header(HttpHeaders.RETRY_AFTER,  String.valueOf(e.getRetryAfterSeconds()))
            .body(problem);
    }
}