package com.nido.api.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * An error refused by a servlet filter and an error thrown inside a controller travel through
 * different code, but they are the same thing to whoever receives them, and they used to arrive
 * looking different.
 *
 * <p>Spring Boot 4 serialises responses with Jackson 3, while the security handlers held a Jackson 2
 * {@code ObjectMapper} built by hand. Jackson 3 carries Spring's own knowledge of
 * {@code ProblemDetail} — it drops the default {@code type} and folds away the extension map;
 * a bare Jackson 2 mapper knows none of that and reflected the record as it stands, trailing a
 * {@code "properties":null} that no other error in this API has ever had.
 *
 * <p>Small, and it was noticed by someone reading a 401 in a browser rather than by any test, which
 * is the point of this one.
 */
@IntegrationTestConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorFormatConsistencyIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @LocalServerPort int port;

    private HttpResponse<String> get(String path) {
        try {
            return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void an_error_from_the_filter_chain_carries_no_field_the_rest_of_the_api_lacks() {
        // /api/spaces is authenticated, so an anonymous call is refused by the entry point — the
        // filter path, the one that had its own mapper.
        HttpResponse<String> refusal = get("/api/spaces");

        assertThat(refusal.statusCode()).isEqualTo(401);
        assertThat(refusal.body())
            .as("the extension map is Spring's own and is not part of the wire format")
            .doesNotContain("properties");
        assertThat(refusal.body()).contains("\"status\":401", "\"title\":\"Unauthorized\"");
    }

    @Test
    void the_filter_and_the_controller_describe_an_error_with_the_same_keys() {
        // Same class, two writers. What is held here is that the two agree, not what either says.
        HttpResponse<String> fromFilter = get("/api/spaces");
        HttpResponse<String> fromController = get("/api/does-not-exist");

        assertThat(fromFilter.body()).contains("\"status\":", "\"title\":", "\"detail\":", "\"instance\":");
        assertThat(fromController.body()).contains("\"status\":", "\"title\":", "\"detail\":", "\"instance\":");
        assertThat(fromController.body()).doesNotContain("properties");
    }

    @Test
    void both_are_announced_as_problem_json() {
        assertThat(get("/api/spaces").headers().firstValue("content-type"))
            .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
        assertThat(get("/api/does-not-exist").headers().firstValue("content-type"))
            .hasValueSatisfying(type -> assertThat(type).startsWith("application/problem+json"));
    }
}
