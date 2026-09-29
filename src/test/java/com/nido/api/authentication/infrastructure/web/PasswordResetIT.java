package com.nido.api.authentication.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.MailIntegrationTestConfig;
import com.nido.api.SharedGreenMail;
import com.nido.api.TestHashUtils;
import com.nido.api.authentication.infrastructure.persistence.entity.PasswordResetTokenEntity;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.PasswordResetTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.nido.api.shared.model.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole flow with mail on: a real mail to a real SMTP server, the link read out of it. */
@MailIntegrationTestConfig
class PasswordResetIT {

    private static final Pattern LINK = Pattern.compile("http://localhost:5173/reset-password#token=([A-Za-z0-9_-]+)");

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired UserTotpJpaRepository totps;
    @Autowired PasswordResetTokenJpaRepository resetTokens;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired TotpEncryptorFactory encryptorFactory;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private UserIdentityEntity jane;

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
        jane = createUser("jane", "jane@test.com", "OldPassw0rd!", true, null);
    }

    /**
     * The dispatcher is live and shared by every mail-on IT: a mail a test never waited for (the
     * "password changed" one, say) would land in the next test's mailbox after its purge. The outbox
     * row goes once the mail is delivered, so an empty outbox means nothing is left in flight.
     */
    @AfterEach
    void letEveryMailLeave() throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        while (jdbc.sql("SELECT count(*) FROM mail_outbox").query(Long.class).single() > 0
            && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
    }

    private UserIdentityEntity createUser(String username, String email, String password, boolean active, String language) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setActive(active);
        user.setLanguage(language);
        users.saveAndFlush(user);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(user.getId());
        credential.setPasswordHash(encoder.encode(password));
        credentials.save(credential);
        return user;
    }

    private MvcResult requestReset(String identifier, String acceptLanguage) throws Exception {
        return mockMvc.perform(post("/api/auth/password-reset/request")
                .header("Accept-Language", acceptLanguage)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"identifier\":\"" + identifier + "\"}"))
            .andExpect(status().isAccepted())
            .andExpect(content().string(""))
            .andReturn();
    }

    private MimeMessage onlyMail() throws Exception {
        assertThat(SharedGreenMail.server().waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage[] received = SharedGreenMail.server().getReceivedMessages();
        assertThat(received).hasSize(1);
        return received[0];
    }

    private static String textOf(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String text = textOf(multipart.getBodyPart(i));
                if (text != null) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String tokenIn(MimeMessage mail) throws Exception {
        Matcher link = LINK.matcher(textOf(mail));
        assertThat(link.find()).as("a reset link in the mail").isTrue();
        return link.group(1);
    }

    private Cookie loginAs(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn().getResponse().getCookie("refresh_token");
    }

    private int login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn().getResponse().getStatus();
    }

    private void check(String token, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/check")
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
            .andExpect(status().is(expectedStatus));
    }

    private void confirm(String token, String password, int expectedStatus) throws Exception {
        // The browser says which language it speaks; an account with none recorded is mailed in it.
        mockMvc.perform(post("/api/auth/password-reset/confirm")
                .header("Accept-Language", "fr")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"))
            .andExpect(status().is(expectedStatus));
    }

    @Test
    void the_app_is_told_password_reset_exists() throws Exception {
        mockMvc.perform(get("/api/auth/capabilities"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.passwordReset").value(true));
    }

    @Test
    void forgotten_then_reset_end_to_end() throws Exception {
        requestReset("jane", "fr");

        MimeMessage mail = onlyMail();
        assertThat(((InternetAddress) mail.getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress()).isEqualTo("jane@test.com");
        assertThat(mail.getSubject()).isEqualTo("Réinitialiser votre mot de passe Nido");
        String token = tokenIn(mail);
        assertThat(resetTokens.findAll()).singleElement()
            .satisfies(stored -> assertThat(stored.getTokenHash()).isEqualTo(TestHashUtils.sha256(token)).isNotEqualTo(token));

        check(token, 204);
        confirm(token, "NewPassw0rd!", 204);

        assertThat(login("jane", "OldPassw0rd!")).isEqualTo(401);
        assertThat(login("jane", "NewPassw0rd!")).isEqualTo(200);
    }

    @Test
    void a_link_works_once() throws Exception {
        requestReset("jane", "fr");
        String token = tokenIn(onlyMail());

        confirm(token, "NewPassw0rd!", 204);

        check(token, 410);
        confirm(token, "OtherPassw0rd!", 410);
        assertThat(login("jane", "NewPassw0rd!")).isEqualTo(200);
    }

    @Test
    void the_holder_is_told_their_password_changed() throws Exception {
        requestReset("jane", "fr");
        String token = tokenIn(onlyMail());
        SharedGreenMail.server().purgeEmailFromAllMailboxes();

        confirm(token, "NewPassw0rd!", 204);

        assertThat(onlyMail().getSubject()).isEqualTo("Votre mot de passe Nido a été modifié");
    }

    @Test
    void every_session_the_old_password_opened_ends() throws Exception {
        Cookie refresh = loginAs("jane", "OldPassw0rd!");
        Cookie accessFromAMinuteAgo = tokenIssuedAMinuteAgoFor(jane.getId());
        mockMvc.perform(get("/api/users/me").cookie(accessFromAMinuteAgo)).andExpect(status().isOk());
        requestReset("jane", "fr");

        confirm(tokenIn(onlyMail()), "NewPassw0rd!", 204);

        mockMvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/me").cookie(accessFromAMinuteAgo)).andExpect(status().isUnauthorized());
    }

    @Test
    void a_reset_never_gets_past_the_second_factor() throws Exception {
        UserTotpEntity totp = new UserTotpEntity();
        totp.setUserId(jane.getId());
        totp.setTotpSecret(encryptorFactory.forUser(jane.getId()).encrypt("JBSWY3DPEHPK3PXP"));
        totp.setTotpEnabled(true);
        totps.save(totp);
        requestReset("jane", "fr");

        confirm(tokenIn(onlyMail()), "NewPassw0rd!", 204);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("jane", "NewPassw0rd!"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpRequired").value(true));
    }

    @Test
    void the_answer_is_the_same_for_a_stranger_a_deactivated_account_and_a_real_one() throws Exception {
        createUser("off", "off@test.com", "OldPassw0rd!", false, null);

        requestReset("nobody-at-all", "fr");
        requestReset("off", "fr");

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 1)).isFalse();
        requestReset("jane", "fr");
        onlyMail();
    }

    @Test
    void an_address_in_any_letter_case_reaches_the_account() throws Exception {
        requestReset("JANE@Test.com", "fr");

        assertThat(((InternetAddress) onlyMail().getRecipients(MimeMessage.RecipientType.TO)[0]).getAddress())
            .isEqualTo("jane@test.com");
    }

    @Test
    void the_mail_speaks_the_holders_language_not_the_requesters() throws Exception {
        createUser("john", "john@test.com", "OldPassw0rd!", true, "en");

        requestReset("john", "fr");

        assertThat(onlyMail().getSubject()).isEqualTo("Reset your Nido password");
    }

    @Test
    void without_a_recorded_language_the_request_decides() throws Exception {
        requestReset("jane", "en-GB,en;q=0.9");

        assertThat(onlyMail().getSubject()).isEqualTo("Reset your Nido password");
    }

    @Test
    void asking_again_within_five_minutes_sends_nothing_more() throws Exception {
        requestReset("jane", "fr");
        onlyMail();

        requestReset("jane", "fr");

        assertThat(SharedGreenMail.server().waitForIncomingEmail(1_500, 2)).isFalse();
    }

    @Test
    void an_expired_link_is_refused() throws Exception {
        Instant past = Instant.now().minusSeconds(3600);
        resetTokens.save(new PasswordResetTokenEntity(jane.getId(), TestHashUtils.sha256("expired"), past, past.plusSeconds(1800)));

        check("expired", 410);
        confirm("expired", "NewPassw0rd!", 410);
    }

    @Test
    void a_weak_password_is_refused_and_the_link_survives() throws Exception {
        requestReset("jane", "fr");
        String token = tokenIn(onlyMail());

        confirm(token, "weak", 400);

        check(token, 204);
    }

    @Test
    void asking_six_times_in_fifteen_minutes_is_refused() throws Exception {
        for (int i = 0; i < 5; i++) {
            requestReset("stranger-" + i, "fr");
        }
        mockMvc.perform(post("/api/auth/password-reset/request")
                .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"stranger-6\"}"))
            .andExpect(status().isTooManyRequests());
    }

    /** A token minted a minute ago — a live session, as in UserControllerIT. */
    private static Cookie tokenIssuedAMinuteAgoFor(UUID userId) {
        Instant issuedAt = Instant.now().minusSeconds(60);
        String token = Jwts.builder()
            .issuer("nido").audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusSeconds(15 * 60L)))
            .signWith(Keys.hmacShaKeyFor("integration-test-secret-at-least-32-chars!".getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
