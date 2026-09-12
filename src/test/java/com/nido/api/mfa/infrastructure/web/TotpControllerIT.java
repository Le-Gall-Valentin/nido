package com.nido.api.mfa.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TotpPolicy;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.security.CustomUserDetails;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.mfa.domain.port.out.PendingTotpEnrolmentPort;
import com.nido.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.IntegrationTestConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
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
    @Autowired PendingTotpEnrolmentPort pendingEnrolment;
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
    void setup_calledTwice_handsBackTheSameSecretInsteadOfANewOne() throws Exception {
        // The enrolment secret is sticky until it is confirmed or deleted: saveTotpSecretIfAbsent
        // is an UPDATE ... WHERE totp_secret IS NULL, so a second call writes nothing and the
        // handler returns what is already stored. Deliberate — two tabs on the enrolment page must
        // not show two different QR codes, or the user scans one and confirms against the other.
        //
        // Pinned here because the OpenAPI description used to promise the opposite ("un nouveau
        // secret est généré et remplace l'ancien"), which left anyone who lost their phone mid
        // enrolment stuck with a secret they could no longer use and no documented way out.
        Cookie access = loginAs("testuser", "password");

        String first = objectMapper.readTree(
            mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("secret").asText();
        String second = objectMapper.readTree(
            mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("secret").asText();

        assertThat(second).isEqualTo(first);
    }

    @Test
    void setup_pendingEnrolment_cannotBeCancelledByItsOwner() throws Exception {
        // The consequence of the stickiness above, pinned because it is a dead end rather than a
        // design: DELETE /api/auth/2fa requires a valid code, and a pending enrolment has no
        // enabled TOTP to produce one from. Somebody who loses their phone between /setup and
        // /confirm therefore cannot start over on their own — only an admin reset
        // (POST /api/users/{id}/2fa/reset) or five deliberately wrong confirmations clear it.
        // The answer is 409 TotpNotEnabled: from the API's point of view there is nothing to
        // disable, even though a secret is sitting in the database blocking any fresh enrolment.
        // Documented in the endpoint description; see the audit note before "fixing" this test.
        Cookie access = loginAs("testuser", "password");
        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/auth/2fa").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"123456\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void adminReset_nowClearsAPendingEnrolment_soAStuckUserHasAWayOut() throws Exception {
        // This used to answer 204 and change nothing: AdminTotpDisableService only acted
        // `if (profile.totpEnabled())`, and a pending enrolment is by definition not enabled, so
        // the one recourse a stuck user could be pointed at did nothing while telling the admin it
        // had worked. Somebody who loses their phone mid-enrolment can now be unblocked.
        Cookie access = loginAs("testuser", "password");
        String before = objectMapper.readTree(
            mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
                .andReturn().getResponse().getContentAsString()).get("secret").asText();
        String targetId = userIdentityJpaRepository.findByUsername("testuser").orElseThrow().getId().toString();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(loginAs("superadmin", "adminpass")))
            .andExpect(status().isNoContent());

        String after = objectMapper.readTree(
            mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
                .andReturn().getResponse().getContentAsString()).get("secret").asText();
        assertThat(after)
            .as("the pending enrolment is cleared, so setup starts over")
            .isNotEqualTo(before);
    }

    @Test
    void confirming_promotes_the_enrolment_from_redis_into_the_account() throws Exception {
        // The moment the whole refactor turns on: the secret only existed for the length of the
        // enrolment, and confirming is what writes it where it will survive. If that write were
        // missed, the account would report 2FA enabled with no secret to check codes against.
        Cookie access = loginAs("testuser", "password");
        String secret = objectMapper.readTree(
            mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
                .andReturn().getResponse().getContentAsString()).get("secret").asText();

        mockMvc.perform(post("/api/auth/2fa/confirm").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + currentCodeFor(secret) + "\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/2fa/status").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpEnabled").value(true));

        UserTotpEntity stored = userTotpJpaRepository.findById(
            userIdentityJpaRepository.findByUsername("testuser").orElseThrow().getId()).orElseThrow();
        assertThat(stored.getTotpSecret())
            .as("the proven secret is persisted, encrypted, by the confirmation")
            .isNotNull().isNotEqualTo(secret);
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
    void verify_locksTheAccountAfter5FailedAttempts() throws Exception {
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
        // The title distinguishes this from the rate limiter, which answers 429 as well.
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

    /** The code the user's authenticator app would be showing right now for this secret. */
    private static String currentCodeFor(String secret) throws Exception {
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        return generator.generate(secret, Math.floorDiv(System.currentTimeMillis() / 1000L, 30));
    }

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
    // ─── B3 : le verrou anti-brute-force doit survivre au rollback ─────────

    @Test
    void the_pending_secret_is_really_gone_after_the_confirmation_attempts_run_out() throws Exception {
        // ConfirmTotpHandler discards the pending secret and then throws, which used to roll its
        // transaction back — the discard was undone while the Redis attempt counter, reset in the
        // same breath and not transactional, stayed at zero. The lockout announced by the 429 never
        // happened: the caller got five fresh guesses, then five more, without limit.
        //
        // The enrolment now lives in Redis alongside that counter, so the two can no longer come
        // apart and the REQUIRES_NEW that used to paper over it is gone. The property is unchanged
        // and still worth holding — it is only read somewhere else.
        Cookie access = loginAs("testuser", "password");
        java.util.UUID userId = userIdentityJpaRepository.findByUsername("testuser").orElseThrow().getId();

        mockMvc.perform(post("/api/auth/2fa/setup").cookie(access))
            .andExpect(status().isOk());
        assertThat(pendingEnrolment.find(userId)).isPresent();

        for (int attempt = 1; attempt < TotpPolicy.MAX_ATTEMPTS; attempt++) {
            mockMvc.perform(post("/api/auth/2fa/confirm").cookie(access)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/2fa/confirm").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isTooManyRequests());

        assertThat(pendingEnrolment.find(userId)).isEmpty();
        // With no pending secret left, confirming is no longer a guessing game at all:
        // restarting the setup is the only way forward.
        mockMvc.perform(post("/api/auth/2fa/confirm").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000000\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
    // ─── B4 : le compteur d'échecs suit le compte, pas le challenge ────────

    @Test
    void verify_theLockoutSurvivesLoggingInAgainForAFreshChallenge() throws Exception {
        // The bypass: the attempt counter used to hang off the challenge id, and every login
        // mints a new one — so using up five attempts and logging back in handed the caller a
        // clean slate, five guesses at a time, without limit. Rate limits capped that at five
        // guesses a minute per IP, which a distributed caller sidesteps entirely.
        Cookie firstChallenge = loginAndGetChallengeCookie("totpuser", "totppass");
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/2fa/verify").cookie(firstChallenge)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"00000" + i + "\"}"))
                .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/2fa/verify").cookie(firstChallenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000005\"}"))
            .andExpect(status().isTooManyRequests());

        // Cleared so the assertion below cannot pass on the rate limiter's own 429 — the point
        // is the account lockout, and the two are otherwise indistinguishable by status alone.
        rateLimitBucketStore.clearAll();
        Cookie secondChallenge = loginAndGetChallengeCookie("totpuser", "totppass");
        assertThat(secondChallenge).isNotNull();
        assertThat(secondChallenge.getValue()).isNotEqualTo(firstChallenge.getValue());

        mockMvc.perform(post("/api/auth/2fa/verify").cookie(secondChallenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"000006\"}"))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.title").value("AuthenticationError"));
    }

    @Test
    void verify_aSuccessfulCodeClearsTheAccountCounter() throws Exception {
        // Someone who fumbles a few codes before getting one right must not carry those
        // failures forward — otherwise the lockout would creep up on ordinary use.
        Cookie firstChallenge = loginAndGetChallengeCookie("totpuser", "totppass");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/2fa/verify").cookie(firstChallenge)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"00000" + i + "\"}"))
                .andExpect(status().isUnauthorized());
        }

        rateLimitBucketStore.clearAll();
        Cookie secondChallenge = loginAndGetChallengeCookie("totpuser", "totppass");
        DefaultCodeGenerator generator = new DefaultCodeGenerator(HashingAlgorithm.SHA256, 6);
        long counter = Math.floorDiv(System.currentTimeMillis() / 1000L, 30);
        mockMvc.perform(post("/api/auth/2fa/verify").cookie(secondChallenge)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + generator.generate(KNOWN_TOTP_SECRET, counter) + "\"}"))
            .andExpect(status().isOk());

        rateLimitBucketStore.clearAll();
        Cookie thirdChallenge = loginAndGetChallengeCookie("totpuser", "totppass");
        // Four failures in a row must all read as a wrong code. Had the three earlier ones
        // survived, the second of these would have been the fifth and answered 429.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/2fa/verify").cookie(thirdChallenge)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"code\":\"10000" + i + "\"}"))
                .andExpect(status().isUnauthorized());
        }
    }
}
