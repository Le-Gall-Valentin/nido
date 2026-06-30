package com.synapsys.api.authentication.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.shared.model.Role;
import com.synapsys.api.authentication.infrastructure.persistence.entity.RefreshTokenEntity;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.TestHashUtils;
import jakarta.servlet.http.Cookie;
import com.synapsys.api.mfa.infrastructure.config.TotpEncryptorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class AuthControllerIT {

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

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@test.com");
        user.setRole(Role.USER);
        userIdentityJpaRepository.save(user);

        UserCredentialEntity userCred = new UserCredentialEntity();
        userCred.setUserId(user.getId());
        userCred.setPasswordHash(encoder.encode("password"));
        userCredentialJpaRepository.save(userCred);

        UserIdentityEntity admin = new UserIdentityEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setRole(Role.SUPER_ADMIN);
        userIdentityJpaRepository.save(admin);

        UserCredentialEntity adminCred = new UserCredentialEntity();
        adminCred.setUserId(admin.getId());
        adminCred.setPasswordHash(encoder.encode("adminpass"));
        userCredentialJpaRepository.save(adminCred);

        UserIdentityEntity totpUser = new UserIdentityEntity();
        totpUser.setUsername("totpuser");
        totpUser.setEmail("totpuser@test.com");
        totpUser.setRole(Role.USER);
        userIdentityJpaRepository.save(totpUser);

        UserCredentialEntity totpCred = new UserCredentialEntity();
        totpCred.setUserId(totpUser.getId());
        totpCred.setPasswordHash(encoder.encode("totppass"));
        userCredentialJpaRepository.save(totpCred);

        UserTotpEntity totpEntity = new UserTotpEntity();
        totpEntity.setUserId(totpUser.getId());
        totpEntity.setTotpSecret(encryptorFactory.forUser(totpUser.getId()).encrypt("JBSWY3DPEHPK3PXP"));
        totpEntity.setTotpEnabled(true);
        userTotpJpaRepository.save(totpEntity);
    }

    @Test
    void login_success_returnUserInfoAndSetsTwoHttpOnlyCookies() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn();

        Cookie access = result.getResponse().getCookie("access_token");
        Cookie refresh = result.getResponse().getCookie("refresh_token");

        assertThat(access).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();
        assertThat(access.getPath()).isEqualTo("/api");

        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(refresh.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void login_success_setsRateLimitHeaders() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-RateLimit-Limit"))
            .andExpect(header().exists("X-RateLimit-Remaining"))
            .andExpect(header().exists("X-RateLimit-Reset"))
            .andExpect(header().doesNotExist("Retry-After"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpassword"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validToken_rotatesAndSetsNewCookies() throws Exception {
        Cookie firstRefresh = loginAndGetCookie("refresh_token");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isNoContent())
            .andReturn();

        assertThat(result.getResponse().getCookie("access_token")).isNotNull();
        assertThat(result.getResponse().getCookie("refresh_token")).isNotNull();

        assertThat(refreshTokenJpaRepository.findByTokenHash(sha256(firstRefresh.getValue())))
            .isPresent()
            .get()
            .extracting(RefreshTokenEntity::isRevoked)
            .isEqualTo(true);
    }

    @Test
    void refresh_withNoRefreshCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedToken_revokesAllAndReturns401() throws Exception {
        Cookie firstRefresh = loginAndGetCookie("refresh_token");

        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
            .andExpect(status().isUnauthorized());

        assertThat(refreshTokenJpaRepository.findAll())
            .allMatch(RefreshTokenEntity::isRevoked);
    }

    @Test
    void logout_revokesTokenAndClearsCookies() throws Exception {
        MvcResult loginResult = login();
        Cookie access = loginResult.getResponse().getCookie("access_token");
        Cookie refresh = loginResult.getResponse().getCookie("refresh_token");

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                .cookie(access).cookie(refresh))
            .andExpect(status().isNoContent())
            .andReturn();

        Cookie clearedAccess = logoutResult.getResponse().getCookie("access_token");
        Cookie clearedRefresh = logoutResult.getResponse().getCookie("refresh_token");
        assertThat(clearedAccess).isNotNull();
        assertThat(clearedAccess.getMaxAge()).isZero();
        assertThat(clearedRefresh).isNotNull();
        assertThat(clearedRefresh.getMaxAge()).isZero();

        assertThat(refreshTokenJpaRepository.findByTokenHash(sha256(refresh.getValue())))
            .isPresent()
            .get()
            .extracting(RefreshTokenEntity::isRevoked)
            .isEqualTo(true);
    }

    @Test
    void unauthenticatedRequest_returns401WithProblemDetailFormat() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("about:blank"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void login_whenIpRateLimitExceeded_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .with(req -> { req.setRemoteAddr("198.51.100.1"); return req; })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpass"))))
                .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                .with(req -> { req.setRemoteAddr("198.51.100.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "wrongpass"))))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("X-RateLimit-Limit", "5"))
            .andExpect(header().string("X-RateLimit-Remaining", "0"))
            .andExpect(header().exists("X-RateLimit-Reset"))
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void refresh_whenIpRateLimitExceeded_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/refresh")
                    .with(req -> { req.setRemoteAddr("198.51.100.2"); return req; }))
                .andExpect(status().isUnauthorized()); // pas de cookie refresh → 401
        }

        mockMvc.perform(post("/api/auth/refresh")
                .with(req -> { req.setRemoteAddr("198.51.100.2"); return req; }))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"));
    }

    @Test
    void login_whenTotpEnabled_returnsTotpRequiredAndSetsChallengeHttpOnlyCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpRequired").value(true))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("totp_challenge")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/api/auth/2fa")));
    }

    @Test
    void verifyTotp_withNonDigitCode_returns400() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andReturn();
        Cookie challenge = loginResult.getResponse().getCookie("totp_challenge");

        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ABCDEF\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyTotp_withTooLongCode_returns400() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andReturn();
        Cookie challenge = loginResult.getResponse().getCookie("totp_challenge");

        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"1234567\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void verifyTotp_withoutChallengeCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/2fa/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"123456\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyTotp_withInvalidCode_returns401() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("totpuser", "totppass"))))
            .andExpect(status().isOk())
            .andReturn();

        Cookie challenge = loginResult.getResponse().getCookie("totp_challenge");
        assertThat(challenge).isNotNull();

        mockMvc.perform(post("/api/auth/2fa/verify")
                .cookie(challenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnauthorized());
    }

    MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andReturn();
    }

    Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }

    Cookie loginAndGetCookie(String name) throws Exception {
        return login().getResponse().getCookie(name);
    }

    String sha256(String raw) {
        return TestHashUtils.sha256(raw);
    }
}