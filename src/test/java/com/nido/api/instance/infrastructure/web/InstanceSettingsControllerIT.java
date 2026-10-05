package com.nido.api.instance.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.TestAccessTokens;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.shared.model.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
@ExtendWith(SharedGreenMail.Starter.class)
class InstanceSettingsControllerIT {

    @Autowired WebApplicationContext context;
    @Autowired SettingsStorePort store;
    @Autowired RedisRateLimitBucketStore buckets;

    private MockMvc mvc;
    private final UUID superAdminId = UUID.randomUUID();
    private Cookie superAdmin;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        buckets.clearAll();
        superAdmin = TestAccessTokens.cookieFor(superAdminId, Role.SUPER_ADMIN);
    }

    @AfterEach
    void clean() {
        InstanceSettingsTestSupport.clear(store);
    }

    @Test
    void only_a_super_admin_reads_the_settings() throws Exception {
        mvc.perform(get("/api/admin/settings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/settings").cookie(TestAccessTokens.cookieFor(UUID.randomUUID(), Role.ADMIN)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").cookie(superAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groups[0].group").value("mail"))
            .andExpect(jsonPath("$.groups[3].fields[0].key").value("api.swagger"))
            .andExpect(jsonPath("$.groups[3].fields[0].value").value("false"))
            .andExpect(jsonPath("$.groups[3].fields[0].source").value("DEFAULT"))
            .andExpect(jsonPath("$.groups[3].fields[0].variable").value("SWAGGER_ENABLED"));
    }

    @Test
    void a_block_is_saved_reported_and_reset() throws Exception {
        mvc.perform(put("/api/admin/settings/sessions").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"values\":{\"sessions.access-token-minutes\":\"30\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groups[2].fields[0].value").value("30"))
            .andExpect(jsonPath("$.groups[2].fields[0].source").value("DATABASE"));

        mvc.perform(delete("/api/admin/settings/sessions/sessions.access-token-minutes").cookie(superAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.groups[2].fields[0].source").value("DEFAULT"));
    }

    @Test
    void wrong_values_come_back_by_setting() throws Exception {
        mvc.perform(put("/api/admin/settings/sessions").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"values\":{\"sessions.access-token-minutes\":\"abc\"}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("SETTINGS_INVALID"))
            .andExpect(jsonPath("$.errors['sessions.access-token-minutes']").value("not_a_number"));
    }

    @Test
    void an_unknown_block_or_setting_is_not_found() throws Exception {
        mvc.perform(put("/api/admin/settings/nope").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"values\":{}}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/settings/api/mail.host").cookie(superAdmin)).andExpect(status().isNotFound());
    }

    @Test
    void mail_configured_from_the_page_sends_the_test_to_the_administrator() throws Exception {
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        mvc.perform(put("/api/admin/settings/public-url").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"values\":{\"public-url\":\"http://nido.test/\"}}")).andExpect(status().isOk());

        String mail = "{\"values\":{\"mail.host\":\"127.0.0.1\",\"mail.port\":\"" + SharedGreenMail.PORT
            + "\",\"mail.security\":\"none\",\"mail.from\":\"Nido <nido@test.local>\"}}";
        mvc.perform(post("/api/admin/settings/mail/test").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON).content(mail))
            .andExpect(status().isNoContent());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(5_000, 1)).isTrue();
        assertThat(SharedGreenMail.server().getReceivedMessages()[0].getAllRecipients()[0].toString())
            .isEqualTo(superAdminId + "@test.local");
    }

    @Test
    void a_server_that_refuses_is_reported_as_it_said() throws Exception {
        mvc.perform(put("/api/admin/settings/public-url").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"values\":{\"public-url\":\"http://nido.test\"}}")).andExpect(status().isOk());

        mvc.perform(post("/api/admin/settings/mail/test").cookie(superAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"values\":{\"mail.host\":\"127.0.0.1\",\"mail.port\":\"1\",\"mail.security\":\"none\",\"mail.from\":\"nido@test.local\"}}"))
            .andExpect(status().is(422))
            .andExpect(jsonPath("$.error_code").value("MAIL_TEST_FAILED"))
            .andExpect(jsonPath("$.reason").value("connection_refused"));
    }
}
