package com.synapsys.api.infrastructure.ratelimit;

import com.synapsys.api.shared.infrastructure.web.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

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