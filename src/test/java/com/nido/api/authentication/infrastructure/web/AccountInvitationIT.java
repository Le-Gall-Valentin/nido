package com.nido.api.authentication.infrastructure.web;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail is off in this context: the invitation link is handed back, and the routes work all the same. */
@IntegrationTestConfig
class AccountInvitationIT {

    private static final String PASSWORD = "Welcome-Home-1";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired InviteAccountUseCase invite;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private String username;
    private UUID userId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        username = "inv-" + UUID.randomUUID().toString().substring(0, 8);
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.local");
        user.setRole(Role.USER);
        userId = users.saveAndFlush(user).getId();
    }

    private String invited() {
        InvitationDelivery delivery = invite.invite(userId, "root");
        assertThat(delivery).isInstanceOf(InvitationDelivery.Link.class);
        String url = ((InvitationDelivery.Link) delivery).url();
        assertThat(url).contains("/welcome#token=");
        return url.substring(url.indexOf("token=") + "token=".length());
    }

    private ResultActions check(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/account-invitation/check")
            .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"));
    }

    private ResultActions accept(String token, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/account-invitation/accept")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"identifier\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    private int invitationsOfTheAccount() {
        return jdbc.sql("SELECT count(*) FROM account_invitations WHERE user_id = :id")
            .param("id", userId).query(Integer.class).single();
    }

    @Test
    void the_link_names_its_account_sets_its_first_password_and_the_account_signs_in() throws Exception {
        String token = invited();

        check(token).andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username));
        login(PASSWORD).andExpect(status().isUnauthorized());
        accept(token, PASSWORD).andExpect(status().isNoContent());

        login(PASSWORD).andExpect(status().isOk());
        assertThat(invitationsOfTheAccount()).isZero();
    }

    @Test
    void a_link_works_once() throws Exception {
        String token = invited();
        accept(token, PASSWORD).andExpect(status().isNoContent());

        accept(token, "Another-Home-2").andExpect(status().isGone())
            .andExpect(jsonPath("$.error_code").value("invitation_link_invalid"));
        check(token).andExpect(status().isGone());
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void a_password_the_rules_refuse_keeps_the_link_usable() throws Exception {
        String token = invited();

        accept(token, "weak").andExpect(status().isBadRequest());

        check(token).andExpect(status().isOk());
    }

    @Test
    void an_expired_link_is_refused_and_stays_where_it_was() throws Exception {
        String token = invited();
        jdbc.sql("UPDATE account_invitations SET expires_at = now() - interval '1 minute' WHERE user_id = :id")
            .param("id", userId).update();

        check(token).andExpect(status().isGone());
        accept(token, PASSWORD).andExpect(status().isGone());
        // The refusal rolled the consumption back: the administration still shows an expired invitation.
        assertThat(invitationsOfTheAccount()).isOne();
    }

    @Test
    void the_link_of_a_deactivated_account_waits_for_its_reactivation() throws Exception {
        String token = invited();

        jdbc.sql("UPDATE users SET is_active = false WHERE id = :id").param("id", userId).update();
        check(token).andExpect(status().isGone());

        jdbc.sql("UPDATE users SET is_active = true WHERE id = :id").param("id", userId).update();
        check(token).andExpect(status().isOk());
    }

    @Test
    void a_new_link_replaces_the_previous_one() throws Exception {
        String first = invited();
        String second = invited();

        check(first).andExpect(status().isGone());
        check(second).andExpect(status().isOk());
    }

    @Test
    void the_app_is_told_mail_is_off() throws Exception {
        mockMvc.perform(get("/api/auth/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mail").value(false));
    }

    @Test
    void checking_a_link_is_limited_to_ten_requests_a_window_per_address() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/account-invitation/check").with(from("198.51.100.31"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"guess-" + i + "\"}"))
                .andExpect(status().isGone());
        }

        mockMvc.perform(post("/api/auth/account-invitation/check").with(from("198.51.100.31"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"guess-10\"}"))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void accepting_a_link_is_limited_to_ten_requests_a_window_per_address() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/account-invitation/accept").with(from("198.51.100.32"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"token\":\"guess-" + i + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isGone());
        }

        mockMvc.perform(post("/api/auth/account-invitation/accept").with(from("198.51.100.32"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"guess-10\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isTooManyRequests());
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
