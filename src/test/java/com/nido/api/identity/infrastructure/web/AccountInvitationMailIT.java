package com.nido.api.identity.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nido.api.ReceivedMails.allTo;
import static com.nido.api.ReceivedMails.subjectOf;
import static com.nido.api.ReceivedMails.textOf;
import static com.nido.api.ReceivedMails.to;
import static com.nido.api.TestAccessTokens.cookieFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail on: an account created from the administration is invited by mail, and joins with the link. */
@MailIntegrationTestConfig
class AccountInvitationMailIT {

    private static final Pattern LINK = Pattern.compile("http://localhost:5173/welcome#token=([A-Za-z0-9_-]+)");
    private static final String PASSWORD = "Welcome-Home-1";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private String suffix;
    private UUID rootId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        UserIdentityEntity root = new UserIdentityEntity();
        root.setUsername("root-" + suffix);
        root.setEmail("root-" + suffix + "@test.local");
        root.setRole(Role.SUPER_ADMIN);
        rootId = users.saveAndFlush(root).getId();
    }

    @AfterEach
    void forgetTheMails() {
        jdbc.sql("DELETE FROM mail_outbox").update();
        // Out of the super-administrators that later tests' administrators' gestures would tell.
        jdbc.sql("UPDATE users SET is_active = false WHERE id = :id").param("id", rootId).update();
    }

    private String name(String who) {
        return who + "-" + suffix;
    }

    private String address(String who) {
        return name(who) + "@test.local";
    }

    private String create(String who) throws Exception {
        String body = mockMvc.perform(post("/api/users").cookie(cookieFor(rootId, Role.SUPER_ADMIN))
                .header(HttpHeaders.ACCEPT_LANGUAGE, "fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name(who) + "\",\"email\":\"" + address(who) + "\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.invitation.delivery").value("mail"))
            .andExpect(jsonPath("$.invitation.link").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static String tokenIn(MimeMessage mail) throws Exception {
        Matcher link = LINK.matcher(textOf(mail));
        assertThat(link.find()).as("an invitation link in the mail").isTrue();
        return link.group(1);
    }

    private ResultActions check(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/account-invitation/check")
            .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"));
    }

    @Test
    void the_invitation_is_mailed_in_the_admin_s_language_and_its_link_leads_to_a_sign_in() throws Exception {
        create("carol");

        MimeMessage mail = to(address("carol"));
        assertThat(subjectOf(mail)).isEqualTo(name("root") + " vous invite à rejoindre Nido");
        assertThat(textOf(mail)).contains("Votre identifiant : " + name("carol"));
        String token = tokenIn(mail);

        check(token).andExpect(status().isOk()).andExpect(jsonPath("$.username").value(name("carol")));
        mockMvc.perform(post("/api/auth/account-invitation/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + name("carol") + "\",\"password\":\"" + PASSWORD + "\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void forgot_password_on_an_invited_account_mails_a_new_link_and_the_first_one_stops_working() throws Exception {
        String carolId = create("carol");
        String first = tokenIn(to(address("carol")));
        // Past the five minutes between two links.
        jdbc.sql("UPDATE account_invitations SET created_at = now() - interval '10 minutes' WHERE user_id = :id")
            .param("id", UUID.fromString(carolId)).update();

        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"" + name("carol") + "\"}"))
            .andExpect(status().isAccepted());

        MimeMessage renewed = allTo(address("carol"), 2).get(1);
        // No language on the account and none asked by this request: English, the app's own fallback.
        assertThat(subjectOf(renewed)).isEqualTo("Your new link to join Nido");
        check(first).andExpect(status().isGone());
        check(tokenIn(renewed)).andExpect(status().isOk());
    }
}
