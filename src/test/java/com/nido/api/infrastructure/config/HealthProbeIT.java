package com.nido.api.infrastructure.config;

import com.nido.api.IntegrationTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.contributor.HealthContributor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The probe the container uses to decide whether this process is still worth sending traffic to.
 *
 * <p>Adding actuator adds endpoints, and endpoints are surface. Three things therefore have to hold
 * at once, and only the first is what the feature is for: the probe answers, the application port
 * does not serve it, and the answer says nothing beyond up or down.
 *
 * <p>The second matters because of how this is deployed — nginx forwards the application port and
 * only that, so an endpoint there is an endpoint on the public internet. Keeping management on its
 * own unpublished port is what makes the probe reachable by the container and by nothing else, and
 * that separation is worth a test rather than a comment: it holds today by configuration, and
 * configuration is what changes.
 *
 * <p>The third is why {@code show-details} is off. A health report names every backing service and
 * whether it is reachable — Postgres, Redis, disk — which is a map of the deployment handed to
 * whoever asks. A probe only ever needed one bit.
 */
@IntegrationTestConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "management.server.port=0")
class HealthProbeIT {

    @Autowired Map<String, HealthContributor> healthContributors;

    @LocalServerPort int applicationPort;
    @LocalManagementPort int managementPort;

    private HttpResponse<String> get(int port, String path) {
        try {
            return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void the_probe_answers_on_the_management_port_without_credentials() {
        // Docker runs this from inside the container, with no session to present.
        HttpResponse<String> response = get(managementPort, "/actuator/health");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void the_application_port_does_not_serve_the_probe() {
        // The one that keeps this from becoming a public endpoint: nginx forwards this port.
        //
        // Asserted on the body rather than the status, because the status is 200 and says nothing:
        // the SPA fallback answers any extensionless GET with index.html, so /actuator/health on the
        // public host returns the single-page application, exactly as /v3/api-docs did. What matters
        // is that no health report comes back, and none does.
        HttpResponse<String> response = get(applicationPort, "/actuator/health");

        assertThat(response.body())
            .as("actuator on the application port would be actuator on the public host")
            .doesNotContain("\"status\":\"UP\"")
            .doesNotContain("\"status\"");
    }

    @Test
    void the_answer_carries_no_map_of_the_deployment() {
        HttpResponse<String> response = get(managementPort, "/actuator/health");

        assertThat(response.body())
            .as("which services back this application, and which of them are down, is not a probe's business")
            .doesNotContain("components", "db", "redis", "diskSpace", "PostgreSQL");
    }

    @Test
    void the_probe_is_wired_to_the_database_it_would_have_to_report_on() {
        // A probe that answers UP whatever happens is worse than none: the orchestrator keeps
        // sending traffic to a process that cannot serve it, and the light stays green. What is
        // asserted is the mechanism rather than the outcome — the database is registered as a
        // contributor, which is how the aggregate can ever be DOWN.
        //
        // Breaking the database to watch the report change was the first attempt and it was a bad
        // test: pausing the container does not close the port, it swallows the packets, so the
        // health check sat waiting on a network timeout and the suite hung past ten minutes.
        // Whether Boot folds a failing contributor into a 503 is Boot's own test suite's business.
        assertThat(healthContributors.keySet())
            .anySatisfy(name -> assertThat(name).containsIgnoringCase("db"));
    }

    @Test
    void redis_is_deliberately_not_part_of_the_verdict() {
        // Redis is a hard dependency — the application refuses to start without it, and it holds
        // the TOTP challenges, the rate-limit buckets, the token cut-offs and the pending
        // enrolments. It is still left out of what this probe answers, because the probe's
        // consequence is a restart, and restarting this container does not bring Redis back: it
        // trades a partly working application for a crash loop. Losing Redis is something to be
        // paged about, not something to restart into.
        //
        // Pinned rather than left implicit: it is absent today because nothing registers it, and
        // an upgrade that starts registering one would change what a restart means here.
        assertThat(healthContributors.keySet())
            .noneSatisfy(name -> assertThat(name).containsIgnoringCase("redis"));
    }

    @Test
    void nothing_but_health_is_exposed() {
        // Actuator ships env, beans, mappings, configprops, heapdump and more. Exposing health is
        // asking for one of them, not for the set.
        for (String endpoint : new String[]{"/actuator/env", "/actuator/beans", "/actuator/mappings",
                                            "/actuator/configprops", "/actuator/loggers", "/actuator"}) {
            assertThat(get(managementPort, endpoint).statusCode())
                .as("%s must not be served", endpoint)
                .isNotEqualTo(200);
        }
    }
}
