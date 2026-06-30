package com.synapsys.api.identity.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapsys.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.synapsys.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.synapsys.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.synapsys.api.authentication.infrastructure.web.dto.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.synapsys.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.synapsys.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.synapsys.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.synapsys.api.IntegrationTestConfig;
import com.synapsys.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.synapsys.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.synapsys.api.shared.model.Role;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class UserControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserCredentialJpaRepository userCredentialJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
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
        userIdentityJpaRepository.saveAndFlush(user);
        saveCredential(user.getId(), "password");

        UserIdentityEntity admin = new UserIdentityEntity();
        admin.setUsername("superadmin");
        admin.setEmail("superadmin@test.com");
        admin.setRole(Role.SUPER_ADMIN);
        userIdentityJpaRepository.saveAndFlush(admin);
        saveCredential(admin.getId(), "adminpass");

        UserIdentityEntity totpUser = new UserIdentityEntity();
        totpUser.setUsername("totpuser");
        totpUser.setEmail("totpuser@test.com");
        totpUser.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(totpUser);
        saveCredential(totpUser.getId(), "totppass");
        saveTotpRecord(totpUser.getId(), "JBSWY3DPEHPK3PXP", true);

        UserIdentityEntity inactiveUser = new UserIdentityEntity();
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setEmail("inactive@test.com");
        inactiveUser.setRole(Role.USER);
        inactiveUser.setActive(false);
        userIdentityJpaRepository.saveAndFlush(inactiveUser);
        saveCredential(inactiveUser.getId(), "inactivepass");

        UserIdentityEntity adminUser = new UserIdentityEntity();
        adminUser.setUsername("adminuser");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
        userIdentityJpaRepository.saveAndFlush(adminUser);
        saveCredential(adminUser.getId(), "adminpass2");
    }

    @Test
    void me_withValidAccessToken_returnsUserInfo() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("testuser@test.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.totpEnabled").value(false));
    }

    @Test
    void me_withTotpEnabled_returnsTotpEnabledTrue() throws Exception {
        UserIdentityEntity totpUser = userIdentityJpaRepository.findByUsername("totpuser").get();
        // Build a valid access token directly — loginAs would be blocked by TOTP challenge
        Instant now = Instant.now();
        String token = Jwts.builder()
            .issuer("synapsys-api")
            .audience().add("synapsys-api").and()
            .subject(totpUser.getId().toString())
            .claim("role", totpUser.getRole().name())
            .claim("email", totpUser.getEmail())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(15 * 60L)))
            .signWith(Keys.hmacShaKeyFor(
                "integration-test-secret-at-least-32-chars!".getBytes(StandardCharsets.UTF_8)))
            .compact();
        Cookie access = new Cookie("access_token", token);

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totpEnabled").value(true))
            .andExpect(jsonPath("$.username").value("totpuser"))
            .andExpect(jsonPath("$.email").value("totpuser@test.com"));
    }

    @Test
    void me_withoutCookie_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTamperedToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .cookie(new Cookie("access_token", "eyJhbGciOiJIUzI1NiJ9.tampered.signature")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asSuperAdmin_createsUser() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/users/[0-9a-f-]+")))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.email").value("newuser@test.com"))
            .andExpect(jsonPath("$.totpEnabled").value(false))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void register_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void register_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"password\":\"securepass\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"other@test.com\",\"password\":\"Securepass1!\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void register_withoutRole_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"brandnewuser\",\"email\":\"brand@test.com\",\"password\":\"Securepass1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"simpleuser\",\"email\":\"simple@test.com\",\"password\":\"simplepass\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_superAdminCannotCreateSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newsa\",\"email\":\"newsa@test.com\",\"password\":\"Securepass1!\",\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asSuperAdmin_gdprDeletesUser_returns204() throws Exception {
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        saveTotpRecord(targetId, "JBSWY3DPEHPK3PXP", false);
        loginAs("testuser", "password"); // creates a refresh token for testuser
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isNoContent());

        UserIdentityEntity entity = userIdentityJpaRepository.findById(targetId).get();
        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getUsername()).isNull();
        assertThat(entity.getEmail()).isNull();
        assertThat(userCredentialJpaRepository.findById(targetId)).isEmpty();
        assertThat(userTotpJpaRepository.findById(targetId)).isEmpty();
        boolean hasTokens = refreshTokenJpaRepository.findAll().stream()
            .anyMatch(t -> targetId.equals(t.getUserId()));
        assertThat(hasTokens).isFalse();
    }

    @Test
    void deleteUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/" + UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUser_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + UUID.randomUUID()).cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_self_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID selfId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + selfId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_adminCannotDeleteSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID superAdminId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + superAdminId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_superAdminCannotDeleteSuperAdmin_returns403() throws Exception {
        UserIdentityEntity secondSuperAdmin = new UserIdentityEntity();
        secondSuperAdmin.setUsername("superadmin2");
        secondSuperAdmin.setEmail("superadmin2@test.com");
        secondSuperAdmin.setRole(Role.SUPER_ADMIN);
        userIdentityJpaRepository.saveAndFlush(secondSuperAdmin);

        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + secondSuperAdmin.getId()).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void resetTotp_asSuperAdmin_resetsTotpForUser_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("totpuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void resetTotp_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findByUsername("totpuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void resetTotp_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/users/" + UUID.randomUUID() + "/2fa/reset"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void resetTotp_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users/" + UUID.randomUUID() + "/2fa/reset").cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_authenticated_updatesUsernameAndEmail_returns204() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@test.com\"}"))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByUsername("updated")).isPresent();
        assertThat(userIdentityJpaRepository.findByEmail("updated@test.com")).isPresent();
    }

    @Test
    void updateProfile_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"superadmin\",\"email\":\"unique@test.com\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_duplicateEmail_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"uniqueuser\",\"email\":\"superadmin@test.com\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_invalidUsername_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\",\"email\":\"ok@test.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"x\",\"email\":\"x@test.com\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_inactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"updated\",\"email\":\"updated@test.com\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void changePassword_correctCurrentPassword_returns204() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void changePassword_wrongCurrentPassword_returns422() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"weak\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"x\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withInactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(get("/api/users/me").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateProfile_invalidEmail_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"notanemail\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_sameAsCurrentPassword_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"Samepass1!\",\"newPassword\":\"Samepass1!\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_inactiveUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID testUserId = userIdentityJpaRepository.findByUsername("testuser").get().getId();
        userIdentityJpaRepository.deactivateById(testUserId);

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"Newpassword1!\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_asSuperAdmin_returns200WithPage() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.content[0].id").isNotEmpty())
            .andExpect(jsonPath("$.content[0].role").isNotEmpty())
            .andExpect(jsonPath("$.content[0].isActive").isBoolean())
            .andExpect(jsonPath("$.content[0].totpEnabled").isBoolean())
            .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
    }

    @Test
    void listUsers_totpEnabledUser_appearsWithTotpEnabledTrue() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.username == 'totpuser')].totpEnabled").value(true));
    }

    @Test
    void listUsers_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(get("/api/users").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_invalidSortBy_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("sortBy", "invalid").cookie(access))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_sizeOverMax_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("size", "101").cookie(access))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_negativePage_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("page", "-1").cookie(access))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listUsers_searchMatchesUsernameAndEmail() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        // "admin" matches superadmin (username) and adminuser (username + admin@test.com)
        mockMvc.perform(get("/api/users").param("search", "admin").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[?(@.username == 'superadmin')]").exists())
            .andExpect(jsonPath("$.content[?(@.username == 'adminuser')]").exists());
    }

    @Test
    void listUsers_searchIsCaseInsensitive() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("search", "INACTIVE").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].username").value("inactiveuser"));
    }

    @Test
    void listUsers_searchMatchesEmailOnly() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        // "@test.com" appears in every seeded email but in no username
        mockMvc.perform(get("/api/users").param("search", "@test.com").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void listUsers_searchNoMatch_returnsEmptyPage() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("search", "zzz-no-match").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listUsers_searchLikeWildcardsAreEscaped() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        // '%' must be treated literally, not as a LIKE wildcard matching everything
        mockMvc.perform(get("/api/users").param("search", "%").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listUsers_searchTooLong_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("search", "a".repeat(255)).cookie(access))
            .andExpect(status().isBadRequest());
    }

    @Test
    void activateUser_asSuperAdmin_activatesInactiveUser_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID inactiveId = userIdentityJpaRepository.findByUsername("inactiveuser").get().getId();

        mockMvc.perform(post("/api/users/" + inactiveId + "/activate").cookie(access))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(inactiveId).get().isActive()).isTrue();
    }

    @Test
    void activateUser_alreadyActive_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID activeId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(post("/api/users/" + activeId + "/activate").cookie(access))
            .andExpect(status().isConflict());
    }

    @Test
    void activateUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID inactiveId = userIdentityJpaRepository.findByUsername("inactiveuser").get().getId();

        mockMvc.perform(post("/api/users/" + inactiveId + "/activate").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void activateUser_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users/" + UUID.randomUUID() + "/activate").cookie(access))
            .andExpect(status().isNotFound());
    }

    @Test
    void deactivateUser_asSuperAdmin_deactivatesUser_returns204() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/deactivate").cookie(access))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(targetId).get().isActive()).isFalse();
    }

    @Test
    void deactivateUser_adminOnSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID superAdminId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(post("/api/users/" + superAdminId + "/deactivate").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deactivateUser_alreadyInactive_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID inactiveId = userIdentityJpaRepository.findByUsername("inactiveuser").get().getId();

        mockMvc.perform(post("/api/users/" + inactiveId + "/deactivate").cookie(access))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUser_superAdminPromotesUserToAdmin_returns204AndPersists() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(targetId).get().getRole())
            .isEqualTo(Role.ADMIN);
    }

    @Test
    void updateUser_superAdminDemotesAdminToUser_returns204AndPersists() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("adminuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(targetId).get().getRole())
            .isEqualTo(Role.USER);
    }

    @Test
    void updateUser_adminTargetsUserWithSameRole_returns409() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUser_adminCannotUpdateAdmin_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UserIdentityEntity secondAdmin = new UserIdentityEntity();
        secondAdmin.setUsername("adminuser2");
        secondAdmin.setEmail("adminuser2@test.com");
        secondAdmin.setRole(Role.ADMIN);
        userIdentityJpaRepository.saveAndFlush(secondAdmin);

        mockMvc.perform(patch("/api/users/" + secondAdmin.getId())
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_adminCannotAssignAdminRole_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_noneCanAssignSuperAdminRole_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findByUsername("adminuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUser_nonExistentUser_returns404() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(patch("/api/users/" + UUID.randomUUID())
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_self_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID selfId = userIdentityJpaRepository.findByUsername("superadmin").get().getId();

        mockMvc.perform(patch("/api/users/" + selfId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_missingBody_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_invalidRoleValue_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"UNKNOWN_ROLE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_sameRole_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findByUsername("adminuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUser_inactiveTarget_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID inactiveId = userIdentityJpaRepository.findByUsername("inactiveuser").get().getId();

        mockMvc.perform(patch("/api/users/" + inactiveId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    private void saveCredential(UUID userId, String rawPassword) {
        UserCredentialEntity cred = new UserCredentialEntity();
        cred.setUserId(userId);
        cred.setPasswordHash(encoder.encode(rawPassword));
        userCredentialJpaRepository.save(cred);
    }

    private void saveTotpRecord(UUID userId, String secret, boolean enabled) {
        UserTotpEntity totp = new UserTotpEntity();
        totp.setUserId(userId);
        totp.setTotpSecret(encryptorFactory.forUser(userId).encrypt(secret));
        totp.setTotpEnabled(enabled);
        userTotpJpaRepository.save(totp);
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }
}