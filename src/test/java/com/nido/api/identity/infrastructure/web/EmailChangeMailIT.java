package com.nido.api.identity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.nido.api.shared.model.Role;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MailIntegrationTestConfig
class EmailChangeMailIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired PasswordResetTokenJpaRepository resetTokens;
    @Autowired UserTotpJpaRepository totps;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimitBucketStore.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        resetTokens.deleteAll();
        refreshTokens.deleteAll();
        totps.deleteAll();
        credentials.deleteAll();
        users.deleteAll();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();

        UserIdentityEntity jane = new UserIdentityEntity();
        jane.setUsername("jane");
        jane.setEmail("jane@old.fr");
        jane.setRole(Role.USER);
        jane.setLanguage("fr");
        users.saveAndFlush(jane);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(jane.getId());
        credential.setPasswordHash(new BCryptPasswordEncoder(12).encode("password"));
        credentials.save(credential);
    }

    private Cookie login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "password"))))
            .andReturn().getResponse().getCookie("access_token");
    }

    @Test
    void the_old_address_hears_about_the_new_one() throws Exception {
        mockMvc.perform(patch("/api/users/me").cookie(login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jane\",\"email\":\"jane.doe@example.fr\",\"currentPassword\":\"password\"}"))
            .andExpect(status().isNoContent());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage mail = SharedGreenMail.server().getReceivedMessages()[0];
        assertThat(((InternetAddress) mail.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress()).isEqualTo("jane@old.fr");
        assertThat(mail.getSubject()).isEqualTo("L’adresse de votre compte Nido a été modifiée");
    }

    @Test
    void a_refused_change_tells_nobody() throws Exception {
        mockMvc.perform(patch("/api/users/me").cookie(login())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"jane\",\"email\":\"jane.doe@example.fr\",\"currentPassword\":\"wrong\"}"))
            .andExpect(status().isUnprocessableEntity());

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
    }
}
