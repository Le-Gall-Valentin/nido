package com.synapsys.api.infrastructure.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitExceptionHandlerTest {

    private RateLimitExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new RateLimitExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/auth/login");
    }

    private RateLimitExceededException exceptionWith(long limit, long resetEpoch, long retryAfter) {
        RateLimitHeaders headers = new RateLimitHeaders(limit, 0L, resetEpoch, retryAfter);
        return new RateLimitExceededException(headers);
    }

    @Test
    void handle_returns429WithProblemDetail() {
        var response = handler.handle(exceptionWith(10L, 9999999999L, 30L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Too many requests. Please try again later.");
        assertThat(response.getBody().getInstance().toString()).isEqualTo("/api/auth/login");
    }

    @Test
    void handle_setsAllRateLimitHeaders() {
        long resetEpoch = 9999999999L;
        var response = handler.handle(exceptionWith(10L, resetEpoch, 30L), request);

        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeaders().getFirst("X-RateLimit-Reset")).isEqualTo(String.valueOf(resetEpoch));
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }
}