package com.nido.api.authentication.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Without NIDO_SMTP_HOST: the app says so, and "forgot password" does not exist. */
@IntegrationTestConfig
class PasswordResetDisabledIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimitBucketStore.clearAll();
    }

    @Test
    void the_app_is_told_there_is_no_password_reset() throws Exception {
        mockMvc.perform(get("/api/auth/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordReset").value(false));
    }

    @Test
    void the_reset_routes_do_not_exist() throws Exception {
        // Mail off: every route answers 404, and nothing was reset or sent.
        for (String route : new String[]{"request", "check", "confirm"}) {
            mockMvc.perform(post("/api/auth/password-reset/" + route)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"jane\",\"token\":\"t\"}"))
                .andExpect(status().isNotFound());
        }
    }
}
