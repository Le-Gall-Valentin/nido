package com.nido.api.notifications.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static com.nido.api.TestAccessTokens.cookieFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail is off in this context: no channel exists, and the card would not show. */
@IntegrationTestConfig
class NotificationPreferencesControllerIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private UUID janeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        String name = "np-" + UUID.randomUUID().toString().substring(0, 8);
        UserIdentityEntity jane = new UserIdentityEntity();
        jane.setUsername(name);
        jane.setEmail(name + "@test.local");
        jane.setRole(Role.USER);
        janeId = users.saveAndFlush(jane).getId();
    }

    @Test
    void mail_off_lists_no_channel() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences").cookie(cookieFor(janeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.channels").isEmpty())
            .andExpect(jsonPath("$.types").isArray());
    }

    @Test
    void every_kind_is_listed_even_without_a_channel_and_on_by_default() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences").cookie(cookieFor(janeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types[?(@.type == 'space.invitation')].enabled").value(true));
    }

    @Test
    void a_channel_this_installation_lacks_cannot_be_switched() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/channels/email").cookie(cookieFor(janeId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("UnknownChannel"));

        assertThat(jdbc.sql("SELECT count(*) FROM notification_channel_preferences WHERE user_id = :id")
            .param("id", janeId).query(Integer.class).single()).isZero();
    }

    @Test
    void the_kinds_reserved_to_super_administrators_are_on_their_card_only() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences").cookie(cookieFor(janeId, Role.ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types[?(@.type == 'identity.account-deactivated')]").isEmpty());
        mockMvc.perform(get("/api/notifications/preferences").cookie(cookieFor(janeId, Role.SUPER_ADMIN)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.types[?(@.type == 'identity.account-deactivated')].enabled").value(true));
    }

    @Test
    void a_kind_reserved_to_super_administrators_cannot_be_switched_by_anyone_else() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/identity.account-deleted").cookie(cookieFor(janeId, Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("UnknownType"));
        mockMvc.perform(put("/api/notifications/preferences/types/identity.account-deleted").cookie(cookieFor(janeId, Role.SUPER_ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void an_unknown_kind_is_not_found_with_its_whole_dotted_code() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/fixture.unknown").cookie(cookieFor(janeId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("UnknownType"))
            .andExpect(jsonPath("$.instance").value("/api/notifications/preferences/types/fixture.unknown"));
    }

    @Test
    void a_switch_without_its_state_is_refused() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/types/fixture.unknown").cookie(cookieFor(janeId))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void nobody_signed_in_is_refused() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences"))
            .andExpect(status().isUnauthorized());
    }}
