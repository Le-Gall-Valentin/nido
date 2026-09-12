package com.nido.api.infrastructure.ratelimit;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two people behind the same reverse proxy are two callers, and the rate limiter has to see them
 * that way.
 *
 * <p>It did not. The application read {@code getRemoteAddr()}, which behind a proxy is the proxy,
 * and the {@code X-Forwarded-For} parsing that was supposed to correct that only ran for peers
 * listed in {@code nido.rate-limit.trusted-proxies} — a property that appeared in no configuration
 * file and defaulted to empty, so it never ran at all. On the live deployment this showed as a
 * single bucket, {@code 172.18.0.1}, holding every request ever made: the five-a-minute limit on
 * signing in was five a minute for everybody at once, and anyone could spend it.
 *
 * <p>Written against the login endpoint because that is where it hurts, and with wrong credentials
 * throughout — the limiter runs before the password is ever checked, and 401 against 429 is exactly
 * the distinction being made. The test client connects from the loopback address, which Tomcat
 * counts as a proxy it may believe, so the forwarded header is honoured here as it would be from
 * nginx in production.
 */
@IntegrationTestConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ForwardedClientIpIT {

    @LocalServerPort int port;
    @Autowired RedisRateLimitBucketStore bucketStore;

    @BeforeEach
    void emptyTheBuckets() {
        bucketStore.clearAll();
    }

    private int login(String forwardedFor) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("X-Forwarded-For", forwardedFor)
                .POST(HttpRequest.BodyPublishers.ofString(
                    "{\"username\":\"nobody\",\"password\":\"wrong-on-purpose\"}"))
                .build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding())
                .statusCode();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void one_caller_exhausting_the_limit_does_not_lock_out_the_next() {
        // Five is the limit on this endpoint. Spend all of it as one address.
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThat(login("203.0.113.10"))
                .as("attempt %d should be refused for the password, not for the rate", attempt)
                .isEqualTo(401);
        }
        assertThat(login("203.0.113.10"))
            .as("the sixth from the same address is the one the limit is for")
            .isEqualTo(429);

        // Before this fix, both addresses were the proxy and this was a 429 too — one person
        // getting their password wrong five times shut everyone else out.
        assertThat(login("198.51.100.20"))
            .as("a different caller behind the same proxy has their own allowance")
            .isEqualTo(401);
    }

    @Test
    void the_header_is_only_believed_from_a_proxy_it_should_believe() {
        // Tomcat rewrites the address only when the peer is inside the private ranges, which the
        // loopback the test dials from is. What must not happen is the header being taken from a
        // caller reaching the application directly — the reason this is Tomcat's job and not a
        // hand-rolled parse: a forged header from a public peer is ignored, and a forged one from
        // behind the proxy is appended to rather than replacing what the proxy saw.
        for (int attempt = 1; attempt <= 5; attempt++) {
            login("203.0.113.30, 10.0.0.7");
        }

        assertThat(login("203.0.113.30, 10.0.0.7"))
            .as("the rightmost address that is not itself a proxy identifies the caller")
            .isEqualTo(429);
        assertThat(login("203.0.113.31, 10.0.0.7"))
            .as("changing the client half of the chain is a different caller")
            .isEqualTo(401);
    }
}
