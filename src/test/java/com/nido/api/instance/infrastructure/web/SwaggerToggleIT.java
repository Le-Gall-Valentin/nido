package com.nido.api.instance.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestAccessTokens;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.model.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class SwaggerToggleIT {

    @Autowired WebApplicationContext context;
    @Autowired SettingsStorePort store;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
    }

    @AfterEach
    void clean() {
        InstanceSettingsTestSupport.clear(store);
    }

    @Test
    void the_documentation_answers_only_while_the_setting_says_so() throws Exception {
        Cookie signedIn = TestAccessTokens.cookieFor(UUID.randomUUID(), Role.USER);

        mvc.perform(get("/api/docs").cookie(signedIn)).andExpect(status().isNotFound());

        store.save(Map.of(SettingKey.SWAGGER, Optional.of("true")), null, Instant.now());
        mvc.perform(get("/api/docs").cookie(signedIn)).andExpect(status().isOk());

        InstanceSettingsTestSupport.clear(store);
        mvc.perform(get("/api/docs").cookie(signedIn)).andExpect(status().isNotFound());
    }

    @Test
    void no_other_spelling_of_the_documentation_answers_while_it_is_off() throws Exception {
        Cookie signedIn = TestAccessTokens.cookieFor(UUID.randomUUID(), Role.USER);

        // springdoc also serves the document as YAML: each of these reaches the documentation unless the
        // switch is checked where MVC matches its routes, not on the raw URI.
        for (String path : new String[]{"/api/docs.yaml", "/api/docs/swagger-config", "/swagger-ui/index.html"}) {
            mvc.perform(get(path).cookie(signedIn)).andExpect(status().isNotFound());
        }
        // A ;parameter, which MVC would drop before matching, is refused outright by Spring Security's firewall.
        mvc.perform(get("/api/docs;x=1").cookie(signedIn)).andExpect(status().isBadRequest());
    }
}
