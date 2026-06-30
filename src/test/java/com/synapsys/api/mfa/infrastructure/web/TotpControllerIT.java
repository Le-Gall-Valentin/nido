package com.synapsys.api.mfa.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.authentication.infrastructure.security.CustomUserDetails;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.synapsys.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class TotpControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired UserCredentialJpaRepository userCredentialJpaRepository;
    @Autowired UserTotpJpaRepository userTotpJpaRepository;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired TotpEncryptorFactory encryptorFactory;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // A known Base32 secret that TOTP apps would accept. The exact value does not matter for
    // tests that only check HTTP responses without verifying a real code.
    private static final String KNOWN_TOTP_SECRET = "JBSWY3DPEHPK3PXP";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        refreshTokenJpaRepository.deleteAll();
        userTotpJpaRepository.deleteAll();
        userCredentialJpaRepository.deleteAll();
        userIdentityJpaRepository.deleteAll();

        // Plain user — no TOTP
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setRole(Role.USER);
        userIdentityJpaRepository.save(user);

        UserCredentialEntity userCred = new UserCredentialEntity();
        userCred.setUserId(user.getId());
        userCred.setPasswordHash(encoder.encode("password"));
        userCredentialJpaRepository.save(userCred);

        UserTotpEntity userTotpRecord = new UserTotpEntity();
        userTotpRecord.setUserId(user.getId());
        userTotpJpaRepository.save(userTotpRecord);

        // Super-admin (for admin-only endpoints)
        UserIdentityEntity admin = new UserIdentityEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setRole(Role.SUPER_ADMIN);
        userIdentityJpaRepository.save(admin);

        UserCredentialEntity adminCred = new UserCredentialEntity();
        adminCred.setUserId(admin.getId());
        adminCred.setPasswordHash(encoder.encode("adminpass"));
        userCredentialJpaRepository.save(adminCred);

        UserTotpEntity adminTotpRecord = new UserTotpEntity();
        adminTotpRecord.setUserId(admin.getId());
        userTotpJpaRepository.save(adminTotpRecord);

        // User with TOTP already enabled
        UserIdentityEntity totpUser = new UserIdentityEntity();
        totpUser.setUsername("totpuser");
        totpUser.setEmail("totpuser@test.com");
        totpUser.setRole(Role.USER);
        userIdentityJpaRepository.save(totpUser);

        UserCredentialEntity totpCred = new UserCredentialEntity();
        totpCred.setUserId(totpUser.getId());
        totpCred.setPasswordHash(encoder.encode("totppass"));
        userCredentialJpaRepository.save(totpCred);

        UserTotpEntity totpRecord = new UserTotpEntity();
        totpRecord.setUserId(totpUser.getId());
        totpRecord.setTotpSecret(encryptorFactory.forUser(totpUser.getId()).encrypt(KNOWN_TOTP_SECRET));
        totpRecord.setTotpEnabled(true);
        userTotpJpaRepository.save(totpRecord);
    }

    // ─── /api/auth/2fa/setup ──────────────────────────────────────────────────

    @Test
    void setup_authenticated_returns200WithOtpauthUriAndSecret() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.otpauthUri").value(startsWith("otpauth://totp/")))
            .andExpect(jsonPath("$.secret").isNotEmpty());
    }

    @Test
    void setup_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/setup"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void setup_alreadyEnabled_returns409() throws Exception {
        // totpuser has totpEnabled=true; logging in as them starts the challenge flow (no access_token).
        // Inject authentication directly to reach the endpoint as an authenticated totpuser.
        String totpUserId = userIdentityJpaRepository.findByUsername("totpuser")
            .orElseThrow().getId().toString();
        CustomUserDetails totpPrincipal = new CustomUserDetails(
            java.util.UUID.fromString(totpUserId), Role.USER, "totpuser@test.com");

        mockMvc.perform(post("/api/auth/2fa/setup")
                .with(user(totpPrincipal)))
            .andExpect(status().isConflict());
    }

    // ─── /api/auth/2fa/confirm ────────────────────────────────────────────────

    @Test
    void confirm_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"123456\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_invalidCode_returns401() throws Exception {
        Cookie access = loginAs("testuser", "password");

        // First call setup so a pending secret exists
        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/2fa/confirm")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── /api/auth/2fa/verify ─────────────────────────────────────────────────

    @Test
    void verify_withValidChallengeAndWrongCode_returns401() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_withoutChallengeCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void verify_challengeLocksAfter5FailedAttempts() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        // First 4 attempts should return 401 (TotpCodeInvalid → "Authentication required")
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/2fa/verify")
                    .cookie(challenge)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"00000" + i + "\"}"))
                .andExpect(status().isUnauthorized());
        }

        // 5th attempt exhausts the counter, invalidates the challenge, and returns TotpMaxAttemptsExceeded (429)
        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000005\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.title").value("AuthenticationError"));
    }

    // ─── /api/auth/2fa (DELETE) ───────────────────────────────────────────────

    @Test
    void disable_totpNotEnabled_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(delete("/api/auth/2fa")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void disable_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/auth/2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ─── Login with TOTP required ─────────────────────────────────────────────

    @Test
    void login_totpRequired_setsTotpChallengeCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpRequired").value(true))
            .andExpect(header().string("Set-Cookie", containsString("totp_challenge")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth/2fa")));
    }

    // ─── /api/auth/2fa/verify — happy path ───────────────────────────────────

    @Test
    void verify_withValidCode_returns200AndSetsJwtCookies() throws Exception {
        Cookie challenge = loginAndGetChallengeCookie("totpuser", "totppass");

        // Generate the current-window TOTP code for the known test secret using SHA-256
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        long counter = Math.floorDiv(System.currentTimeMillis() / 1000L, 30);
        String validCode = generator.generate(KNOWN_TOTP_SECRET, counter);

        MvcResult result = mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + validCode + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("totpuser"))
            .andReturn();

        // Verify JWT cookies are set (multiple Set-Cookie headers — check response cookies directly)
        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();
    }

    @Test
    void status_authenticated_totpNotEnabled_returns200WithFalse() throws Exception {
        Cookie access = loginAs("testuser", "password");
        mockMvc.perform(get("/api/auth/2fa/status").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpEnabled").value(false));
    }

    @Test
    void status_authenticated_totpEnabled_returns200WithTrue() throws Exception {
        String totpUserId = userIdentityJpaRepository.findByUsername("totpuser")
            .orElseThrow().getId().toString();
        CustomUserDetails totpPrincipal = new CustomUserDetails(
            java.util.UUID.fromString(totpUserId), Role.USER, "totpuser@test.com");

        mockMvc.perform(get("/api/auth/2fa/status").with(user(totpPrincipal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpEnabled").value(true));
    }

    @Test
    void confirm_validCode_returns204() throws Exception {
        Cookie access = loginAs("testuser", "password");
        MvcResult setupResult = mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk())
            .andReturn();
        String secret = objectMapper.readTree(setupResult.getResponse().getContentAsString()).get("secret").asText();
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        long counter = Math.floorDiv(System.currentTimeMillis() / 1000L, 30);
        String validCode = generator.generate(secret, counter);
        mockMvc.perform(post("/api/auth/2fa/confirm")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + validCode + "\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void disable_validCode_returns204() throws Exception {
        String totpUserId = userIdentityJpaRepository.findByUsername("totpuser")
            .orElseThrow().getId().toString();
        CustomUserDetails totpPrincipal = new CustomUserDetails(
            java.util.UUID.fromString(totpUserId), Role.USER, "totpuser@test.com");
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        long counter = Math.floorDiv(System.currentTimeMillis() / 1000L, 30);
        String validCode = generator.generate(KNOWN_TOTP_SECRET, counter);
        mockMvc.perform(delete("/api/auth/2fa")
                .with(user(totpPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + validCode + "\"}"))
            .andExpect(status().isNoContent());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    private Cookie loginAndGetChallengeCookie(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("totp_challenge");
    }
}