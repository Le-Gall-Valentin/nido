package com.nido.api.infrastructure.config;

import com.nido.api.IntegrationTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@IntegrationTestConfig
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired MockMvc mockMvc;

    /**
     * The SPA's tab icon: served straight from the classpath with no auth, exactly like
     * index.html itself. A stale filename here (e.g. a leftover ".ico" reference after the
     * icon was switched to an ".svg") falls through every other matcher to denyAll and is
     * silently rejected — reproducible only with Spring Security in the loop, not against
     * the Vite dev server.
     */
    @Test
    void the_favicon_is_reachable_without_authentication() throws Exception {
        mockMvc.perform(get("/favicon.svg"))
            .andExpect(status().is(not(401)))
            .andExpect(status().is(not(403)));
    }
}
