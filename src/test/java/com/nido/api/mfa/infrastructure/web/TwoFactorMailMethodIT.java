package com.nido.api.mfa.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.ReceivedMails;
import com.nido.api.SharedGreenMail;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.Role;
import org.springframework.test.web.servlet.MvcResult;
import com.nido.api.shared.model.TwoFactorMethod;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MailIntegrationTestConfig
class TwoFactorMailMethodIT {

    @Autowired WebApplicationContext context;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired TwoFactorMethodJpaRepository methods;
    @Autowired RedisRateLimitBucketStore rateLimits;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    // One address per test: a mail the previous test queued can still land after the purge.
    private String address;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimits.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        jdbc.sql("DELETE FROM two_factor_mail_codes").update();
        refreshTokens.deleteAll();
        methods.deleteAll();
        credentials.deleteAll();
        users.deleteAll();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();

        UserIdentityEntity jane = new UserIdentityEntity();
        jane.setUsername("jane");
        address = "jane+" + java.util.UUID.randomUUID() + "@example.fr";
        jane.setEmail(address);
        jane.setRole(Role.USER);
        jane.setLanguage("fr");
        users.saveAndFlush(jane);
        redis.delete("totp:mail-sends:user:" + jane.getId());
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(jane.getId());
        credential.setPasswordHash(new BCryptPasswordEncoder(12).encode("password"));
        credentials.save(credential);
    }

    private Cookie login() throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "password"))))
            .andReturn().getResponse().getCookie("access_token");
    }

    private String codeOfMail(int index) throws Exception {
        MimeMessage mail = ReceivedMails.allTo(address, index + 1).get(index);
        Matcher code = Pattern.compile("(?m)^(\\d{6})\\s*$").matcher(ReceivedMails.textOf(mail));
        assertThat(code.find()).as("a code on a line of its own").isTrue();
        return code.group(1);
    }

    @Test
    void the_mail_method_is_turned_on_with_the_code_it_sends_then_off_with_another() throws Exception {
        Cookie access = login();

        mockMvc.perform(post("/api/auth/2fa/mail/setup").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sentTo").value(address))
            .andExpect(jsonPath("$.resendAfterSeconds").value(60));
        mockMvc.perform(post("/api/auth/2fa/mail/confirm").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + codeOfMail(0) + "\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/2fa").cookie(access))
            .andExpect(jsonPath("$[1].enabled").value(true))
            .andExpect(jsonPath("$[1].usable").value(true));

        // mail 1 is "code par mail activé"; mail 2 the code to turn it off
        mockMvc.perform(post("/api/auth/2fa/mail/disable-code").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resendAfterSeconds").value(60));
        mockMvc.perform(delete("/api/auth/2fa/mail").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + codeOfMail(2) + "\"}"))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/2fa").cookie(access))
            .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    void asking_again_within_a_minute_says_when_to_ask() throws Exception {
        Cookie access = login();
        mockMvc.perform(post("/api/auth/2fa/mail/setup").cookie(access)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/2fa/mail/setup").cookie(access))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error_code").value("resend_too_soon"));
    }

    @Test
    void a_wrong_code_does_not_turn_the_mail_on() throws Exception {
        Cookie access = login();
        mockMvc.perform(post("/api/auth/2fa/mail/setup").cookie(access)).andExpect(status().isOk());
        String right = codeOfMail(0);
        String wrong = right.equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/auth/2fa/mail/confirm").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + wrong + "\"}"))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/2fa").cookie(access))
            .andExpect(jsonPath("$[1].enabled").value(false));
    }

    @Test
    void five_wrong_codes_end_the_enrolment_even_though_each_answer_is_an_error() throws Exception {
        // The failures are kept with the code, in the request's transaction: an error that rolled it back would
        // undo the count, and the right code would still work after any number of guesses.
        Cookie access = login();
        mockMvc.perform(post("/api/auth/2fa/mail/setup").cookie(access)).andExpect(status().isOk());
        String right = codeOfMail(0);
        String wrong = right.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/2fa/mail/confirm").cookie(access).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + wrong + "\"}"))
                .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/2fa/mail/confirm").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + wrong + "\"}"))
            .andExpect(status().isTooManyRequests());

        mockMvc.perform(post("/api/auth/2fa/mail/confirm").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + right + "\"}"))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void wrong_codes_to_turn_the_mail_off_stay_counted_and_the_fifth_spends_the_code() throws Exception {
        // Each wrong answer is an error that rolls nothing back: without that, guessing would never end.
        methods.save(new TwoFactorMethodEntity(users.findNotDeletedByUsernameIgnoreCase("jane").orElseThrow().getId(),
            TwoFactorMethod.MAIL, null));
        Cookie access = loginWithMailCode();
        mockMvc.perform(post("/api/auth/2fa/mail/disable-code").cookie(access)).andExpect(status().isOk());
        String right = codeOfMail(1);
        String wrong = right.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 4; i++) {
            rateLimits.clearAll();
            mockMvc.perform(delete("/api/auth/2fa/mail").cookie(access).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"" + wrong + "\"}"))
                .andExpect(status().isUnauthorized());
        }
        rateLimits.clearAll();
        mockMvc.perform(delete("/api/auth/2fa/mail").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + wrong + "\"}"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error_code").value("code_spent"));
        rateLimits.clearAll();
        mockMvc.perform(delete("/api/auth/2fa/mail").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + right + "\"}"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error_code").value("code_expired"));
        mockMvc.perform(get("/api/auth/2fa").cookie(access))
            .andExpect(jsonPath("$[1].enabled").value(true));
    }

    @Test
    void a_disable_code_that_expired_says_so_and_counts_nothing() throws Exception {
        // Ten minutes gone is not five wrong guesses: the person is told to ask again, not that someone guessed.
        methods.save(new TwoFactorMethodEntity(users.findNotDeletedByUsernameIgnoreCase("jane").orElseThrow().getId(),
            TwoFactorMethod.MAIL, null));
        Cookie access = loginWithMailCode();
        mockMvc.perform(post("/api/auth/2fa/mail/disable-code").cookie(access)).andExpect(status().isOk());
        String right = codeOfMail(1);
        jdbc.sql("UPDATE two_factor_mail_codes SET expires_at = now() - interval '1 second'").update();

        mockMvc.perform(delete("/api/auth/2fa/mail").cookie(access).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + right + "\"}"))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.error_code").value("code_expired"));
        assertThat(jdbc.sql("SELECT failed_attempts FROM two_factor_mail_codes").query(Integer.class).single()).isZero();
    }

    private Cookie loginWithMailCode() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "password"))))
            .andReturn();
        Cookie challenge = login.getResponse().getCookie("two_factor_challenge");
        return mockMvc.perform(post("/api/auth/2fa/verify").cookie(challenge).contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"MAIL\",\"code\":\"" + codeOfMail(0) + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getCookie("access_token");
    }
}
