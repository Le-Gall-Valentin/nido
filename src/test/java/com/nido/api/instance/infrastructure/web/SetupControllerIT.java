package com.nido.api.instance.infrastructure.web;

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

/** The test database was set up by NIDO_SEED_* (IntegrationTestConfig): the setup is over and says so. */
@IntegrationTestConfig
class SetupControllerIT {

    @Autowired WebApplicationContext context;
    @Autowired RedisRateLimitBucketStore buckets;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        buckets.clearAll();
    }

    @Test
    void the_status_is_public_and_says_only_that_it_is_done() throws Exception {
        mvc.perform(get("/api/setup/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.required").value(false))
            .andExpect(jsonPath("$.lockedPublicUrl").doesNotExist());
    }

    @Test
    void every_other_route_answers_as_if_absent() throws Exception {
        mvc.perform(post("/api/setup/verify-code").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"AAAA-AAAA-AAAA\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(post("/api/setup/complete").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"AAAA-AAAA-AAAA","admin":{"username":"intruder","email":"x@example.fr","password":"Str0ng!Password","language":"fr"},
                 "publicUrl":"https://evil.example","mail":null,"encryptionKeySaved":true}"""))
            .andExpect(status().isNotFound());
    }
}
