package com.nido.api.identity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.ReceivedMails;
import com.nido.api.SharedGreenMail;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MailIntegrationTestConfig
class EmailChangeWithMailCodeIT {

    @Autowired WebApplicationContext context;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired PasswordResetTokenJpaRepository resetTokens;
    @Autowired TwoFactorMethodJpaRepository methods;
    @Autowired RedisRateLimitBucketStore rateLimits;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private UUID janeId;
    private Cookie access;
    // New addresses of their own per test: a mail the previous test queued can still land after the purge.
    private String newAddress;
    private String otherAddress;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        rateLimits.clearAll();
        jdbc.sql("DELETE FROM mail_outbox").update();
        jdbc.sql("DELETE FROM two_factor_mail_codes").update();
        resetTokens.deleteAll();
        refreshTokens.deleteAll();
        methods.deleteAll();
        credentials.deleteAll();
        users.deleteAll();
        SharedGreenMail.server().purgeEmailFromAllMailboxes();
        String run = UUID.randomUUID().toString();
        newAddress = "jane@new-" + run + ".fr";
        otherAddress = "jane@other-" + run + ".fr";

        UserIdentityEntity jane = new UserIdentityEntity();
        jane.setUsername("jane");
        jane.setEmail("jane@old.fr");
        jane.setRole(Role.USER);
        jane.setLanguage("fr");
        users.saveAndFlush(jane);
        janeId = jane.getId();
        redis.delete("totp:mail-sends:user:" + janeId);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(janeId);
        credential.setPasswordHash(new BCryptPasswordEncoder(4).encode("password"));
        credentials.save(credential);
        UserIdentityEntity john = new UserIdentityEntity();
        john.setUsername("john");
        john.setEmail("john@taken.fr");
        john.setRole(Role.USER);
        users.saveAndFlush(john);
        methods.save(new TwoFactorMethodEntity(janeId, TwoFactorMethod.MAIL, null));
        access = accessCookie(jane);
    }

    /** Signing in would need the mail code: a token is issued directly, as UserControllerIT does for its TOTP user. */
    private static Cookie accessCookie(UserIdentityEntity user) {
        String token = io.jsonwebtoken.Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(user.getId().toString())
            .claim("role", user.getRole().name())
            .claim("email", user.getEmail())
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(15 * 60L)))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "integration-test-secret-at-least-32-chars!".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }

    private ResultActions change(String address, String code) throws Exception {
        String body = code == null
            ? "{\"username\":\"jane\",\"email\":\"" + address + "\",\"currentPassword\":\"password\"}"
            : "{\"username\":\"jane\",\"email\":\"" + address + "\",\"currentPassword\":\"password\",\"emailCode\":\"" + code + "\"}";
        return mockMvc.perform(patch("/api/users/me").cookie(access).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static MimeMessage mailTo(String address) throws Exception {
        return ReceivedMails.to(address);
    }

    private static String codeIn(MimeMessage mail) throws Exception {
        Matcher code = Pattern.compile("(?m)^(\\d{6})\\s*$").matcher(ReceivedMails.textOf(mail));
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    @Test
    void the_new_address_gets_a_code_and_only_that_code_saves_it() throws Exception {
        change(newAddress, null)
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.emailCodeRequired").value(true))
            .andExpect(jsonPath("$.sentTo").value(newAddress))
            .andExpect(jsonPath("$.resendAfterSeconds").value(60));
        MimeMessage mail = mailTo(newAddress);
        assertThat(mail.getSubject()).isEqualTo("Confirmez votre nouvelle adresse Nido");
        assertThat(users.findById(janeId).orElseThrow().getEmail()).isEqualTo("jane@old.fr");

        change(newAddress, codeIn(mail)).andExpect(status().isNoContent());

        assertThat(users.findById(janeId).orElseThrow().getEmail()).isEqualTo(newAddress);
    }

    @Test
    void a_wrong_code_is_a_400_named_for_the_client() throws Exception {
        change(newAddress, null).andExpect(status().isAccepted());
        String wrong = codeIn(mailTo(newAddress)).equals("000000") ? "111111" : "000000";

        change(newAddress, wrong)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("email_code_invalid"));
        assertThat(users.findById(janeId).orElseThrow().getEmail()).isEqualTo("jane@old.fr");
    }

    @Test
    void wrong_codes_stay_counted_and_the_fifth_ends_the_code() throws Exception {
        change(newAddress, null).andExpect(status().isAccepted());
        String right = codeIn(mailTo(newAddress));
        String wrong = right.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 4; i++) {
            rateLimits.clearAll();
            change(newAddress, wrong).andExpect(jsonPath("$.error_code").value("email_code_invalid"));
        }
        rateLimits.clearAll();
        change(newAddress, wrong)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("email_code_spent"));
        rateLimits.clearAll();

        change(newAddress, right)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("email_code_expired"));
    }

    @Test
    void a_code_that_expired_says_so_not_that_someone_guessed() throws Exception {
        change(newAddress, null).andExpect(status().isAccepted());
        String right = codeIn(mailTo(newAddress));
        jdbc.sql("UPDATE two_factor_mail_codes SET expires_at = now() - interval '1 second'").update();

        change(newAddress, right)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("email_code_expired"));
        assertThat(users.findById(janeId).orElseThrow().getEmail()).isEqualTo("jane@old.fr");
    }

    @Test
    void saving_the_same_change_again_within_the_minute_says_when_a_code_can_leave() throws Exception {
        change(newAddress, null).andExpect(status().isAccepted());

        change(newAddress, null)
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error_code").value("resend_too_soon"));
    }

    @Test
    void a_code_for_one_address_does_not_save_another() throws Exception {
        change(newAddress, null).andExpect(status().isAccepted());
        String code = codeIn(mailTo(newAddress));

        change(otherAddress, code).andExpect(status().isBadRequest());
    }

    @Test
    void a_taken_address_is_refused_and_no_code_leaves() throws Exception {
        change("john@taken.fr", null).andExpect(status().isConflict());

        Thread.sleep(1_500);
        assertThat(Arrays.stream(SharedGreenMail.server().getReceivedMessages())
            .filter(mail -> ReceivedMails.recipientOf(mail).equals("john@taken.fr"))).isEmpty();
    }
}
