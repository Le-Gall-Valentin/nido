package com.nido.api.notifications.infrastructure.web;

import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Mail is on: the email channel exists, starts on, and keeps what the account chooses. */
@MailIntegrationTestConfig
class NotificationPreferencesMailIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;
    private Cookie jane;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        String name = "np-" + UUID.randomUUID().toString().substring(0, 8);
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(name);
        user.setEmail(name + "@test.local");
        user.setRole(Role.USER);
        jane = accessTokenFor(users.saveAndFlush(user).getId());
    }

    @Test
    void the_email_channel_is_listed_and_on_by_default() throws Exception {
        mockMvc.perform(get("/api/notifications/preferences").cookie(jane))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.channels.length()").value(1))
            .andExpect(jsonPath("$.channels[0].channel").value("email"))
            .andExpect(jsonPath("$.channels[0].enabled").value(true));
    }

    @Test
    void the_email_channel_stays_off_until_switched_back_on() throws Exception {
        mockMvc.perform(put("/api/notifications/preferences/channels/email").cookie(jane)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/preferences").cookie(jane))
            .andExpect(jsonPath("$.channels[0].enabled").value(false));

        mockMvc.perform(put("/api/notifications/preferences/channels/email").cookie(jane)
                .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications/preferences").cookie(jane))
            .andExpect(jsonPath("$.channels[0].enabled").value(true));
    }

    private Cookie accessTokenFor(UUID userId) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.local")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
