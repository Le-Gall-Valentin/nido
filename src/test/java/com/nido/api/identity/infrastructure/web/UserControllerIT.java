package com.nido.api.identity.infrastructure.web;

import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.authentication.infrastructure.persistence.entity.UserCredentialEntity;
import com.nido.api.authentication.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import com.nido.api.authentication.infrastructure.persistence.repository.UserCredentialJpaRepository;
import com.nido.api.authentication.infrastructure.web.dto.LoginRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.mfa.infrastructure.persistence.entity.UserTotpEntity;
import com.nido.api.mfa.infrastructure.persistence.repository.UserTotpJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceInvitationEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceInvitationJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.nido.api.mfa.infrastructure.config.TotpEncryptorFactory;
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
import java.util.List;
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

    /** 72 characters, 141 bytes: within the character count, past what bcrypt can read. */
    private static final String SEVENTY_TWO_CHARACTERS_OVER_72_BYTES = "Aé1!" + "é".repeat(68);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserCredentialJpaRepository userCredentialJpaRepository;
    @Autowired UserIdentityJpaRepository userIdentityJpaRepository;
    @Autowired RefreshTokenJpaRepository refreshTokenJpaRepository;
    @Autowired UserTotpJpaRepository userTotpJpaRepository;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired TotpEncryptorFactory encryptorFactory;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired SpaceInvitationJpaRepository spaceInvitationJpaRepository;
    @Autowired JdbcClient jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        spaceInvitationJpaRepository.deleteAll();
        spaceJpaRepository.deleteAll();
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
        UserIdentityEntity totpUser = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("totpuser").get();
        // Build a valid access token directly — loginAs would be blocked by TOTP challenge
        Instant now = Instant.now();
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
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
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/users/[0-9a-f-]+")))
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.email").value("newuser@test.com"))
            .andExpect(jsonPath("$.totpEnabled").value(false))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.invitation.delivery").value("link"))
            .andExpect(jsonPath("$.invitation.link").value(org.hamcrest.Matchers.containsString("/welcome#token=")));
    }

    @Test
    void register_aUsernameDifferingOnlyByLetterCase_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"TestUser\",\"email\":\"another@test.com\",\"role\":\"USER\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void register_aUsernameWithAnAtSign_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"new@user\",\"email\":\"newuser@test.com\",\"role\":\"USER\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_aUsernameWithAnAtSign_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test@user\",\"email\":\"testuser@test.com\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void register_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\",\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void register_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"email\":\"newuser@test.com\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"other@test.com\",\"role\":\"USER\"}"))
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
    void register_superAdminCannotCreateSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(post("/api/users")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newsa\",\"email\":\"newsa@test.com\",\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_asSuperAdmin_gdprDeletesUser_returns204() throws Exception {
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
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
    void deleteUser_asSuperAdmin_removesTheInvitationsOfTheDeletedAccount() throws Exception {
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
        UUID adminId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Superadmin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(adminId);
        UUID spaceId = spaceJpaRepository.saveAndFlush(space).getId();

        SpaceInvitationEntity invitation = new SpaceInvitationEntity();
        invitation.setSpaceId(spaceId);
        invitation.setInviteeId(targetId);
        invitation.setRole(SpaceRole.MEMBER);
        invitation.setCode("NIDO-GDPR01");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        invitation.setCreatedBy(adminId);
        UUID invitationId = spaceInvitationJpaRepository.saveAndFlush(invitation).getId();

        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isNoContent());

        assertThat(spaceInvitationJpaRepository.findById(invitationId)).isEmpty();
    }

    @Test
    void deleteUser_asSuperAdmin_forgetsTheNotificationChoicesOfTheDeletedAccount() throws Exception {
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
        UUID adminId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();
        for (UUID userId : List.of(targetId, adminId)) {
            jdbc.sql("INSERT INTO notification_type_preferences VALUES (:id, 'space.invitation', false, now())")
                .param("id", userId).update();
        }
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(delete("/api/users/" + targetId).cookie(access))
            .andExpect(status().isNoContent());

        assertThat(jdbc.sql("SELECT count(*) FROM notification_type_preferences WHERE user_id = :id")
            .param("id", targetId).query(Integer.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM notification_type_preferences WHERE user_id = :id")
            .param("id", adminId).query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    void deleteUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

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
        UUID selfId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

        mockMvc.perform(delete("/api/users/" + selfId).cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_adminCannotDeleteSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID superAdminId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

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
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("totpuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/2fa/reset").cookie(access))
            .andExpect(status().isNoContent());
    }

    @Test
    void resetTotp_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("totpuser").get().getId();

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
                .content("{\"username\":\"updated\",\"email\":\"updated@test.com\",\"currentPassword\":\"password\"}"))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("updated")).isPresent();
        assertThat(userIdentityJpaRepository.findByEmailAndDeletedFalse("updated@test.com")).isPresent();
    }

    @Test
    void updateProfile_newEmailWithoutPassword_returns400() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"elsewhere@test.com\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("CurrentPasswordRequired"));

        assertThat(userIdentityJpaRepository.findByEmailAndDeletedFalse("testuser@test.com")).isPresent();
    }

    @Test
    void updateProfile_newEmailWithWrongPassword_returns422() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"elsewhere@test.com\",\"currentPassword\":\"wrong\"}"))
            .andExpect(status().isUnprocessableEntity());

        assertThat(userIdentityJpaRepository.findByEmailAndDeletedFalse("testuser@test.com")).isPresent();
    }

    @Test
    void updateProfile_usernameOnly_needsNoPassword() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"renamed\",\"email\":\"testuser@test.com\"}"))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateProfile_duplicateUsername_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"superadmin\",\"email\":\"unique@test.com\",\"currentPassword\":\"password\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateProfile_duplicateEmail_returns409() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"uniqueuser\",\"email\":\"superadmin@test.com\",\"currentPassword\":\"password\"}"))
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
        UUID testUserId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
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
    void changePassword_seventyTwoAccentedCharacters_returns400NotA500() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password")
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"" + SEVENTY_TWO_CHARACTERS_OVER_72_BYTES + "\"}"))
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
        UUID testUserId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
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
        UUID testUserId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();
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
        UUID inactiveId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("inactiveuser").get().getId();

        mockMvc.perform(post("/api/users/" + inactiveId + "/activate").cookie(access))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(inactiveId).get().isActive()).isTrue();
    }

    @Test
    void activateUser_alreadyActive_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID activeId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(post("/api/users/" + activeId + "/activate").cookie(access))
            .andExpect(status().isConflict());
    }

    @Test
    void activateUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID inactiveId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("inactiveuser").get().getId();

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
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(post("/api/users/" + targetId + "/deactivate").cookie(access))
            .andExpect(status().isNoContent());

        assertThat(userIdentityJpaRepository.findByIdAndDeletedFalse(targetId).get().isActive()).isFalse();
    }

    @Test
    void deactivateUser_adminOnSuperAdmin_returns403() throws Exception {
        Cookie access = loginAs("adminuser", "adminpass2");
        UUID superAdminId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

        mockMvc.perform(post("/api/users/" + superAdminId + "/deactivate").cookie(access))
            .andExpect(status().isForbidden());
    }

    @Test
    void deactivateUser_alreadyInactive_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID inactiveId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("inactiveuser").get().getId();

        mockMvc.perform(post("/api/users/" + inactiveId + "/deactivate").cookie(access))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUser_superAdminPromotesUserToAdmin_returns204AndPersists() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

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
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("adminuser").get().getId();

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
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

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
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_noneCanAssignSuperAdminRole_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"SUPER_ADMIN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_asUser_returns403() throws Exception {
        Cookie access = loginAs("testuser", "password");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("adminuser").get().getId();

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
        UUID selfId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("superadmin").get().getId();

        mockMvc.perform(patch("/api/users/" + selfId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_missingBody_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_invalidRoleValue_returns400() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"UNKNOWN_ROLE\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_sameRole_returns409() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID targetId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("adminuser").get().getId();

        mockMvc.perform(patch("/api/users/" + targetId)
                .cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"ADMIN\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateUser_inactiveTarget_returns403() throws Exception {
        Cookie access = loginAs("superadmin", "adminpass");
        UUID inactiveId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("inactiveuser").get().getId();

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

    private record Created(String id, String token) {}

    private Created createInvited(String username) throws Exception {
        String body = mockMvc.perform(post("/api/users").cookie(loginAs("superadmin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"email\":\"" + username + "@test.com\",\"role\":\"USER\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return new Created(JsonPath.read(body, "$.id"), tokenOf(JsonPath.read(body, "$.invitation.link")));
    }

    private static String tokenOf(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private org.springframework.test.web.servlet.ResultActions checkInvitation(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/account-invitation/check")
            .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"));
    }

    @Test
    void resendInvitation_replacesTheLink_andTheOldOneNoLongerWorks() throws Exception {
        Created invited = createInvited("newcomer");

        String body = mockMvc.perform(post("/api/users/" + invited.id() + "/invitation")
                .cookie(loginAs("superadmin", "adminpass")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.delivery").value("link"))
            .andReturn().getResponse().getContentAsString();
        String second = tokenOf(JsonPath.read(body, "$.link"));

        checkInvitation(invited.token()).andExpect(status().isGone());
        checkInvitation(second).andExpect(status().isOk());
    }

    @Test
    void resendInvitation_toAnAccountThatChoseItsPassword_returns409() throws Exception {
        UUID testuserId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").orElseThrow().getId();

        mockMvc.perform(post("/api/users/" + testuserId + "/invitation").cookie(loginAs("superadmin", "adminpass")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("AccountAlreadyJoined"));
    }

    @Test
    void resendInvitation_asUser_returns403() throws Exception {
        Created invited = createInvited("newcomer");

        mockMvc.perform(post("/api/users/" + invited.id() + "/invitation").cookie(loginAs("testuser", "password")))
            .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_showsTheInvitationOfAnAccountThatHasNotJoined() throws Exception {
        createInvited("newcomer");
        Cookie access = loginAs("superadmin", "adminpass");

        mockMvc.perform(get("/api/users").param("search", "newcomer").cookie(access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].invitation.status").value("pending"))
            .andExpect(jsonPath("$.content[0].invitation.expiresAt").isNotEmpty());
        mockMvc.perform(get("/api/users").param("search", "testuser").cookie(access))
            .andExpect(jsonPath("$.content[0].invitation").value(org.hamcrest.Matchers.nullValue()));
    }

    private Cookie loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
            .andReturn();
        return result.getResponse().getCookie("access_token");
    }
    // ─── B13 : une perte de droits atteint les tokens déjà émis ───────────

    /** A token minted a minute ago — a live session, not one created in the same instant as the change. */
    private Cookie tokenIssuedAMinuteAgoFor(UUID userId, Role role) {
        Instant issuedAt = Instant.now().minusSeconds(60);
        String token = Jwts.builder()
            .issuer("nido").audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", role.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(issuedAt.plusSeconds(15 * 60L)))
            .signWith(Keys.hmacShaKeyFor("integration-test-secret-at-least-32-chars!".getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }

    @Test
    void demoting_an_admin_stops_the_token_they_are_already_holding() throws Exception {
        // The database says USER a millisecond after the change, but the token in the demoted
        // user's browser keeps saying ADMIN, and nothing in it can ever say otherwise. Before this,
        // it stayed usable on admin routes for the rest of its lifetime — up to fifteen minutes.
        UserIdentityEntity target = new UserIdentityEntity();
        target.setUsername("soon-demoted");
        target.setEmail("soon-demoted@test.com");
        target.setRole(Role.ADMIN);
        userIdentityJpaRepository.saveAndFlush(target);
        Cookie theirToken = tokenIssuedAMinuteAgoFor(target.getId(), Role.ADMIN);

        mockMvc.perform(get("/api/users").cookie(theirToken))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/users/" + target.getId())
                .cookie(loginAs("superadmin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isNoContent());

        // 401 and not 403, deliberately: the frontend refreshes on 401, and refreshing re-reads the
        // user, so the demoted admin carries on as a plain user instead of being thrown out.
        mockMvc.perform(get("/api/users").cookie(theirToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deactivating_an_account_stops_its_current_session_and_not_only_the_next_refresh() throws Exception {
        // Refreshing was already refused for an inactive account, so the account died at the next
        // rotation. What it kept was the token in hand — which is the whole point of deactivating
        // an account you believe is compromised.
        //
        // Checked on /api/spaces and not /api/users/me on purpose: that route re-reads the user and
        // so already refused a deactivated one. A route that does not read the user is where the
        // live token actually bought access — before this, the call below answered 200.
        UserIdentityEntity target = new UserIdentityEntity();
        target.setUsername("soon-disabled");
        target.setEmail("soon-disabled@test.com");
        target.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(target);
        Cookie theirToken = tokenIssuedAMinuteAgoFor(target.getId(), Role.USER);

        mockMvc.perform(get("/api/spaces").cookie(theirToken))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/" + target.getId() + "/deactivate")
                .cookie(loginAs("superadmin", "adminpass")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces").cookie(theirToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleting_an_account_stops_the_token_of_a_user_who_no_longer_exists() throws Exception {
        // The most extreme of the three: everything about the user is erased, yet the token in
        // their browser authenticates fine, because nothing on the request path reads the database.
        // Checked on /api/spaces for the same reason as above — /api/users/me answered 404 anyway,
        // which hides the fact that the token itself was still being accepted.
        UserIdentityEntity target = new UserIdentityEntity();
        target.setUsername("soon-deleted");
        target.setEmail("soon-deleted@test.com");
        target.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(target);
        Cookie theirToken = tokenIssuedAMinuteAgoFor(target.getId(), Role.USER);

        mockMvc.perform(get("/api/spaces").cookie(theirToken))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/" + target.getId())
                .cookie(loginAs("superadmin", "adminpass")))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces").cookie(theirToken))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void a_user_whose_rights_did_not_change_keeps_their_session() throws Exception {
        // The other half: this must cost nothing to everybody else. Demoting one user must not
        // disturb another's live session.
        UserIdentityEntity bystander = new UserIdentityEntity();
        bystander.setUsername("bystander");
        bystander.setEmail("bystander@test.com");
        bystander.setRole(Role.USER);
        userIdentityJpaRepository.saveAndFlush(bystander);
        Cookie theirToken = tokenIssuedAMinuteAgoFor(bystander.getId(), Role.USER);

        UserIdentityEntity target = new UserIdentityEntity();
        target.setUsername("other-demoted");
        target.setEmail("other-demoted@test.com");
        target.setRole(Role.ADMIN);
        userIdentityJpaRepository.saveAndFlush(target);
        mockMvc.perform(patch("/api/users/" + target.getId())
                .cookie(loginAs("superadmin", "adminpass"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"USER\"}"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces").cookie(theirToken))
            .andExpect(status().isOk());
    }

    // ─── B2 : un changement de mot de passe coupe toutes les sessions ──────

    @Test
    void changing_a_password_revokes_every_refresh_token_including_the_caller_s() throws Exception {
        // The point of changing a password is to react to a compromise. A refresh token
        // stolen beforehand outlives the change by up to 30 days unless it is revoked here,
        // which made the change useless against the very case it exists for.
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "password"))))
            .andExpect(status().isOk())
            .andReturn();
        Cookie access = login.getResponse().getCookie("access_token");
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        UUID userId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").orElseThrow().getId();
        assertThat(refreshTokenJpaRepository.findAll())
            .filteredOn(token -> token.getUserId().equals(userId))
            .isNotEmpty()
            .allSatisfy(token -> assertThat(token.isRevoked()).isFalse());

        mockMvc.perform(patch("/api/users/me/password").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"N3wS3cr3t!\"}"))
            .andExpect(status().isNoContent());

        assertThat(refreshTokenJpaRepository.findAll())
            .filteredOn(token -> token.getUserId().equals(userId))
            .isNotEmpty()
            .allSatisfy(token -> assertThat(token.isRevoked()).isTrue());
        // And refused on the wire, not merely flagged in a column.
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void a_failed_password_change_leaves_every_session_alone() throws Exception {
        // Otherwise mistyping the current password — or someone else doing it on a machine
        // left unlocked — would become a way to sign the account out everywhere.
        Cookie access = loginAs("testuser", "password");
        UUID userId = userIdentityJpaRepository.findNotDeletedByUsernameIgnoreCase("testuser").orElseThrow().getId();

        mockMvc.perform(patch("/api/users/me/password").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"N3wS3cr3t!\"}"))
            .andExpect(status().isUnprocessableEntity());

        assertThat(refreshTokenJpaRepository.findAll())
            .filteredOn(token -> token.getUserId().equals(userId))
            .isNotEmpty()
            .allSatisfy(token -> assertThat(token.isRevoked()).isFalse());
    }

    @Test
    void the_new_password_is_the_one_that_works_afterwards() throws Exception {
        Cookie access = loginAs("testuser", "password");

        mockMvc.perform(patch("/api/users/me/password").cookie(access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"password\",\"newPassword\":\"N3wS3cr3t!\"}"))
            .andExpect(status().isNoContent());

        // Revoking every token must not have rolled the password change back with it.
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("testuser", "N3wS3cr3t!"))))
            .andExpect(status().isOk());
    }
}
