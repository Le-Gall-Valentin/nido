package com.nido.api.infrastructure.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.shared.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What an API client actually receives when a route fails in a way nobody planned for.
 *
 * <p>Runs on a real port on purpose: an unhandled exception is finished by the servlet container,
 * which re-dispatches to {@code /error}. MockMvc performs no such dispatch — it rethrows the
 * exception at the caller — so none of the other integration tests can observe any of this, which
 * is exactly why it went unnoticed. Every expectation below was first measured against the code
 * before {@link GlobalExceptionHandler} existed.
 */
@IntegrationTestConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(UnexpectedFailureIT.BoomController.class)
class UnexpectedFailureIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";
    private static final String LEAKY_MESSAGE = "connection pool exhausted at 10.0.0.7:5432";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @LocalServerPort int port;

    private ListAppender<ILoggingEvent> logged;

    /** A route that fails the way real code fails: not with a domain exception anybody declared. */
    @TestConfiguration
    @RestController
    @RequestMapping("/api/test-only/boom")
    static class BoomController {

        @GetMapping
        String onRead() {
            throw new IllegalStateException(LEAKY_MESSAGE);
        }

        @PostMapping
        String onWrite() {
            throw new IllegalStateException(LEAKY_MESSAGE);
        }
    }

    @BeforeEach
    void captureLogs() {
        logged = new ListAppender<>();
        logged.start();
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(logged);
    }

    @AfterEach
    void releaseLogs() {
        ((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).detachAppender(logged);
    }

    /** The JDK client rather than a Spring one: no extra dependency, and no client-side retry. */
    private HttpResponse<String> call(String method, String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Cookie", "access_token=" + tokenFor(UUID.randomUUID()))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String tokenFor(UUID userId) {
        return Jwts.builder()
            .issuer("nido").audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    @Test
    void an_unplanned_failure_answers_in_the_same_format_as_every_other_error() {
        // Before: application/json with {"timestamp","status","error","path"} — Boot's default page,
        // a different shape from the ProblemDetail the rest of the API speaks.
        HttpResponse<String> response = call("GET", "/api/test-only/boom");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(
            type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"title\":\"InternalError\"", "\"status\":500");
    }

    @Test
    void a_write_that_fails_is_a_500_and_not_a_refusal() {
        // A POST leaves the container differently from a GET and the security chain sees the error
        // dispatch too. Checked rather than reasoned about: an authorization-shaped answer to a
        // crash would send both the user and whoever debugs it the wrong way.
        HttpResponse<String> response = call("POST", "/api/test-only/boom");

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).contains("\"title\":\"InternalError\"");
    }

    @Test
    void the_caller_is_never_told_what_actually_broke() {
        HttpResponse<String> response = call("GET", "/api/test-only/boom");

        assertThat(response.body())
            .as("an unplanned exception's message is written for us, not for the caller")
            .doesNotContain(LEAKY_MESSAGE)
            .doesNotContain("10.0.0.7")
            .doesNotContain("IllegalStateException");
    }

    @Test
    void the_incident_is_logged_with_the_request_that_caused_it() {
        // Tomcat logs the stack trace on its own, so the finding's "no ERROR log" was wrong. What
        // was missing is the context: which verb, which route.
        call("POST", "/api/test-only/boom");

        assertThat(logged.list)
            .filteredOn(event -> event.getLevel() == Level.ERROR)
            .singleElement()
            .satisfies(event -> {
                assertThat(event.getFormattedMessage()).contains("POST", "/api/test-only/boom");
                assertThat(event.getThrowableProxy().getMessage()).isEqualTo(LEAKY_MESSAGE);
            });
    }

    @Test
    void an_unknown_api_path_is_a_problem_detail_too_and_still_a_404() {
        // Raised by the SPA fallback as a ResponseStatusException, which used to fall through to the
        // same default page. It must keep its status: a catch-all that turns 404s into 500s would be
        // a worse bug than the one being fixed.
        HttpResponse<String> response = call("GET", "/api/does-not-exist");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(
            type -> assertThat(type).startsWith("application/problem+json"));
    }

    @Test
    void a_bad_request_keeps_its_400_instead_of_becoming_an_incident() {
        // The second face of the catch-all trap, and the one that actually bit: a malformed UUID in
        // the path is Spring's own MethodArgumentTypeMismatchException, which already answers 400.
        // Catching Exception without delegating to ResponseEntityExceptionHandler turned it into a
        // 500 — a client error reported as our fault, and logged as an incident on every typo.
        HttpResponse<String> response = call("GET", "/api/spaces/not-a-uuid");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("content-type")).hasValueSatisfying(
            type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(logged.list).as("a caller's mistake is not an incident").isEmpty();
    }

    @Test
    void a_domain_error_is_untouched_by_the_catch_all() {
        // The bounded contexts' own advices must still win over the last resort.
        HttpResponse<String> response = call("GET", "/api/spaces/" + UUID.randomUUID() + "/finance/categories");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"title\":\"SpaceNotFound\"", "\"detail\":\"Space not found.\"");
        assertThat(logged.list).as("an expected error is not an incident").isEmpty();
    }

    @Test
    void an_unauthenticated_caller_still_gets_401_and_not_500() {
        // The trap of catching Exception: a security failure raised inside the dispatch would be
        // swallowed into a 500 by a naive catch-all.
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/spaces"))
                .GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(401);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
