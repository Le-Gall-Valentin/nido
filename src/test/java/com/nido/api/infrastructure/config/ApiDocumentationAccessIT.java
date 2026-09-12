package com.nido.api.infrastructure.config;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.shared.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

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
 * The API documentation describes every route and every request and response schema. That is a map
 * of the application, and it used to be served to anyone who asked.
 *
 * <p>Run with the documentation switched <b>on</b>, deliberately: with it off there is nothing to
 * leak and the test would prove nothing. What is being held here is that the flag and the access
 * rule are two separate decisions — enabling the documentation must not also publish it. The flag
 * defaults to false, but {@code .env.example} sets it to true, and that is the file people copy.
 *
 * <p>The other half matters just as much and is easy to lose: the session cookie is scoped to
 * {@code /api}, so a document served from anywhere else cannot be read by a logged-in browser at
 * all. Requiring authentication on {@code /v3/api-docs} did not make the documentation private, it
 * made it unreachable — which is why the document now lives under {@code /api} and why the tests
 * below check that a developer still gets it, not only that a stranger does not.
 */
@IntegrationTestConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class ApiDocumentationAccessIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @LocalServerPort int port;

    private HttpResponse<String> get(String path, String cookie) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path)).GET();
            if (cookie != null) {
                request.header("Cookie", cookie);
            }
            return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sessionCookie() {
        Instant now = Instant.now();
        String token = Jwts.builder()
            .issuer("nido").audience().add("nido").and()
            .subject(UUID.randomUUID().toString())
            .claim("role", Role.USER.name())
            .claim("email", "dev@test.com")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return "access_token=" + token;
    }

    @Test
    void the_openapi_document_is_not_served_to_an_anonymous_caller() {
        // Measured before the fix: 200, and 89 KB of it — the complete route list and schemas.
        HttpResponse<String> response = get("/api/docs", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("\"openapi\"").doesNotContain("/api/spaces");
    }

    @Test
    void the_swagger_shell_is_public_but_carries_nothing_about_this_api() {
        // Stock springdoc HTML and JavaScript, byte-identical in every project that uses it. Making
        // it private would only have broken the page: the session cookie is scoped to /api and
        // never reaches /swagger-ui, so an authenticated browser cannot load it either.
        HttpResponse<String> shell = get("/swagger-ui/index.html", null);

        assertThat(shell.statusCode()).isEqualTo(200);
        assertThat(shell.body()).doesNotContain("/api/spaces").doesNotContain("\"openapi\"");
    }

    @Test
    void the_document_the_shell_goes_on_to_fetch_is_the_part_that_is_private() {
        // The chain that matters, and the one the first attempt at this fix broke: the initializer
        // points the page at /api/docs/swagger-config, which is under /api and therefore both
        // authenticated and actually reachable by a logged-in browser.
        assertThat(get("/swagger-ui/swagger-initializer.js", null).body())
            .as("the page must be sent to the protected path, not the old public one")
            .contains("/api/docs/swagger-config");
        assertThat(get("/api/docs/swagger-config", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/docs/swagger-config", sessionCookie()).statusCode()).isEqualTo(200);
    }

    @Test
    void a_logged_in_developer_still_reads_the_documentation() {
        // The other half, and the reason this is authentication rather than a role or a deny: the
        // point of the flag is that somebody wants to read the docs. Swagger UI sends the session
        // cookie, so it keeps working for whoever is logged into the SPA.
        HttpResponse<String> response = get("/api/docs", sessionCookie());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"openapi\"", "/api/spaces");
    }
}
