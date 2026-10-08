package com.nido.api.authentication.infrastructure.web;

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
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
import com.nido.api.mfa.infrastructure.persistence.entity.TwoFactorMethodEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.TwoFactorMethodJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MailIntegrationTestConfig
class TwoFactorLoginIT {

    private static final String SECRET = "JBSWY3DPEHPK3PXP";

    @Autowired WebApplicationContext context;
    @Autowired UserIdentityJpaRepository users;
    @Autowired UserCredentialJpaRepository credentials;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired TwoFactorMethodJpaRepository methods;
    @Autowired TotpEncryptorFactory encryptors;
    @Autowired RedisRateLimitBucketStore rateLimits;
    @Autowired StringRedisTemplate redis;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    // A domain of its own per test: a mail the previous test queued can still land after the purge.
    private String domain;
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
        domain = UUID.randomUUID() + ".example.fr";
        account("mailonly", TwoFactorMethod.MAIL);
        account("both", TwoFactorMethod.APP, TwoFactorMethod.MAIL);
        account("apponly", TwoFactorMethod.APP);
    }

    private void account(String username, TwoFactorMethod... on) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(address(username));
        user.setRole(Role.USER);
        user.setLanguage("fr");
        users.saveAndFlush(user);
        UUID id = user.getId();
        redis.delete("totp:mail-sends:user:" + id);
        redis.delete("totp:attempts:user:" + id);
        UserCredentialEntity credential = new UserCredentialEntity();
        credential.setUserId(id);
        credential.setPasswordHash(new BCryptPasswordEncoder(4).encode("password"));
        credentials.save(credential);
        for (TwoFactorMethod method : on) {
            methods.save(new TwoFactorMethodEntity(id, method,
                method == TwoFactorMethod.APP ? encryptors.forUser(id).encrypt(SECRET) : null));
        }
    }

    private MvcResult login(String username) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, "password"))))
            .andExpect(status().isOk()).andReturn();
    }

    private static Cookie challenge(MvcResult login) {
        return login.getResponse().getCookie("two_factor_challenge");
    }

    private ResultActions verify(Cookie challenge, String method, String code) throws Exception {
        return mockMvc.perform(post("/api/auth/2fa/verify").cookie(challenge).contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"" + method + "\",\"code\":\"" + code + "\"}"));
    }

    private static String appCode() throws Exception {
        return new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6)
            .generate(SECRET, Math.floorDiv(System.currentTimeMillis() / 1000L, 30));
    }

    private String address(String username) {
        return username + "@" + domain;
    }

    private String mailCode(String username, int index) throws Exception {
        MimeMessage mail = ReceivedMails.allTo(address(username), index + 1).get(index);
        Matcher code = Pattern.compile("(?m)^(\\d{6})\\s*$").matcher(ReceivedMails.textOf(mail));
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    private static String wrongThan(String code) {
        return code.equals("000000") ? "111111" : "000000";
    }

    @Test
    void the_mail_alone_sends_its_code_with_the_password_and_the_code_signs_in() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("mailonly", "password"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.twoFactorRequired").value(true))
            .andExpect(jsonPath("$.methods[0]").value("MAIL"))
            .andExpect(jsonPath("$.maskedEmail").value("m••••••y@" + domain))
            .andExpect(jsonPath("$.mailCode.sent").value(true))
            .andExpect(jsonPath("$.mailCode.resendAfterSeconds").value(60))
            .andReturn();

        verify(challenge(login), "MAIL", mailCode("mailonly", 0))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("mailonly"))
            .andExpect(jsonPath("$.twoFactorMethods[0]").value("MAIL"));
    }

    @Test
    void with_both_nothing_leaves_until_the_mail_is_chosen() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("both", "password"))))
            .andExpect(jsonPath("$.methods[0]").value("APP"))
            .andExpect(jsonPath("$.methods[1]").value("MAIL"))
            .andExpect(jsonPath("$.maskedEmail").value("b••••••h@" + domain))
            .andExpect(jsonPath("$.mailCode").doesNotExist())
            .andReturn();
        Thread.sleep(1_500);
        assertThat(Arrays.stream(SharedGreenMail.server().getReceivedMessages())
            .filter(mail -> ReceivedMails.recipientOf(mail).equals(address("both")))).isEmpty();

        mockMvc.perform(post("/api/auth/2fa/challenge/mail").cookie(challenge(login)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sent").value(true))
            .andExpect(jsonPath("$.resendAfterSeconds").value(60));

        verify(challenge(login), "MAIL", mailCode("both", 0)).andExpect(status().isOk());
    }

    @Test
    void with_both_the_app_still_signs_in() throws Exception {
        verify(challenge(login("both")), "APP", appCode())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.twoFactorMethods[0]").value("APP"))
            .andExpect(jsonPath("$.twoFactorMethods[1]").value("MAIL"));
    }

    @Test
    void a_second_login_makes_the_first_code_useless() throws Exception {
        Cookie first = challenge(login("mailonly"));
        String firstCode = mailCode("mailonly", 0);
        Cookie second = challenge(login("mailonly"));
        String secondCode = mailCode("mailonly", 1);

        verify(first, "MAIL", firstCode).andExpect(status().isUnauthorized());
        verify(second, "MAIL", secondCode).andExpect(status().isOk());
    }

    @Test
    void a_method_removed_during_the_challenge_is_refused_by_name() throws Exception {
        Cookie challenge = challenge(login("mailonly"));
        String code = mailCode("mailonly", 0);
        UUID id = users.findNotDeletedByUsernameIgnoreCase("mailonly").orElseThrow().getId();
        methods.deleteAll(methods.findByUserId(id));

        verify(challenge, "MAIL", code)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("method_not_enabled"));
    }

    @Test
    void wrong_codes_count_for_the_account_whatever_the_method() throws Exception {
        Cookie challenge = challenge(login("both"));
        mockMvc.perform(post("/api/auth/2fa/challenge/mail").cookie(challenge)).andExpect(status().isOk());
        String wrongMail = wrongThan(mailCode("both", 0));
        String wrongApp = wrongThan(appCode());

        for (int i = 0; i < 2; i++) {
            verify(challenge, "APP", wrongApp).andExpect(status().isUnauthorized());
            verify(challenge, "MAIL", wrongMail).andExpect(status().isUnauthorized());
        }

        // The account's lockout, not the route's rate limit: the lockout carries no Retry-After.
        verify(challenge, "MAIL", wrongMail)
            .andExpect(status().isTooManyRequests())
            .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void asking_for_the_mail_code_again_too_soon_says_when() throws Exception {
        Cookie challenge = challenge(login("both"));
        mockMvc.perform(post("/api/auth/2fa/challenge/mail").cookie(challenge)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/2fa/challenge/mail").cookie(challenge))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error_code").value("resend_too_soon"));
    }

    @Test
    void the_mail_code_route_needs_a_sign_in_under_way() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/challenge/mail"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error_code").value("two_factor_challenge_expired"));
    }

    @Test
    void an_account_with_the_app_only_is_not_offered_the_mail() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("apponly", "password"))))
            .andExpect(jsonPath("$.methods.length()").value(1))
            .andExpect(jsonPath("$.maskedEmail").doesNotExist())
            .andReturn();

        mockMvc.perform(post("/api/auth/2fa/challenge/mail").cookie(challenge(login)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("method_not_enabled"));
    }
}
