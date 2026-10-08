package com.nido.api.authentication.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail switched off from the settings page while someone is half-way through signing in. */
@IntegrationTestConfig
@ExtendWith(SharedGreenMail.Starter.class)
class TwoFactorLoginMailSwitchIT {

    @Autowired WebApplicationContext context;
    @Autowired SettingsStorePort settings;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired TwoFactorMethodJpaRepository methods;
    @Autowired RedisRateLimitBucketStore rateLimits;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimits.clearAll();
        jdbc.sql("DELETE FROM two_factor_mail_codes").update();
        refreshTokens.deleteAll();
        methods.deleteAll();
        credentials.deleteAll();
        users.deleteAll();
        UserIdentityEntity jane = new UserIdentityEntity();
        jane.setUsername("jane");
        jane.setEmail("jane@example.fr");
        jane.setRole(Role.USER);
        users.saveAndFlush(jane);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(jane.getId());
        credential.setPasswordHash(new BCryptPasswordEncoder(4).encode("password"));
        credentials.save(credential);
        methods.save(new TwoFactorMethodEntity(jane.getId(), TwoFactorMethod.MAIL, null));
        settings.save(Map.of(
            SettingKey.MAIL_HOST, Optional.of("127.0.0.1"),
            SettingKey.MAIL_PORT, Optional.of(String.valueOf(SharedGreenMail.PORT)),
            SettingKey.MAIL_SECURITY, Optional.of("none"),
            SettingKey.MAIL_FROM, Optional.of("Nido <nido@test.local>"),
            SettingKey.PUBLIC_URL, Optional.of("http://nido.test")), null, Instant.now());
    }

    @AfterEach
    void clean() {
        InstanceSettingsTestSupport.clear(settings);
        jdbc.sql("DELETE FROM mail_outbox").update();
    }

    @Test
    void mail_switched_off_during_the_challenge_pauses_the_method_by_name() throws Exception {
        Cookie challenge = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "password"))))
            .andExpect(jsonPath("$.methods[0]").value("MAIL"))
            .andReturn().getResponse().getCookie("two_factor_challenge");

        InstanceSettingsTestSupport.clear(settings);

        mockMvc.perform(post("/api/auth/2fa/verify").cookie(challenge).contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"MAIL\",\"code\":\"123456\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("method_unavailable"));
    }

    @Test
    void with_mail_off_the_password_alone_signs_in_and_the_paused_method_is_named() throws Exception {
        InstanceSettingsTestSupport.clear(settings);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "password"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("jane"))
            .andExpect(jsonPath("$.twoFactorMethods[0]").value("MAIL"));
    }
}
