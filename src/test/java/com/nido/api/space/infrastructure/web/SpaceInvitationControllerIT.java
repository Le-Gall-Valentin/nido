package com.nido.api.space.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceInvitationEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceInvitationJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.identity.infrastructure.web.dto.UpdateProfileRequest;
import com.nido.api.space.infrastructure.web.dto.InviteMemberRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class SpaceInvitationControllerIT {

    // Valeurs exactes de IntegrationTestConfig : tout écart produit un 401 sur tout le fichier.
    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired SpaceInvitationJpaRepository invitations;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID aliceId;
    private UUID bobId;
    private UUID carolId;
    private UUID sharedSpaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        invitations.deleteAll();
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        carolId = saveUser("carol");
        savePersonalSpace(aliceId);
        savePersonalSpace(bobId);
        savePersonalSpace(carolId);
        sharedSpaceId = saveSharedSpace("Chez Valentin", aliceId);
        saveMembership(sharedSpaceId, aliceId, SpaceRole.OWNER);
    }

    @Test
    void invite_an_existing_account_creates_a_pending_invitation() throws Exception {
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.email").value("carol@test.com"))
            .andExpect(jsonPath("$.role").value("MEMBER"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.code", Matchers.matchesPattern("^NIDO-[A-Z0-9]{6}$")));
    }

    @Test
    void listInvitations_renders_the_pending_invitation_with_its_code() throws Exception {
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.MEMBER));
        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/spaces/" + sharedSpaceId + "/invitations").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].email").value("carol@test.com"))
            .andExpect(jsonPath("$[0].code", Matchers.matchesPattern("^NIDO-[A-Z0-9]{6}$")));
    }

    @Test
    void a_plain_member_is_forbidden_on_every_invitation_route() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.MEMBER);
        UUID invitationId = createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId);
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("dave@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(bobId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/spaces/" + sharedSpaceId + "/invitations").cookie(accessTokenFor(bobId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/invitations/" + invitationId)
                .cookie(accessTokenFor(bobId)))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_non_member_gets_404_not_403_on_every_invitation_route() throws Exception {
        UUID invitationId = createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId);
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("dave@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(bobId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/spaces/" + sharedSpaceId + "/invitations").cookie(accessTokenFor(bobId)))
            .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/invitations/" + invitationId)
                .cookie(accessTokenFor(bobId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void inviting_an_address_without_an_account_is_refused() throws Exception {
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("ghost@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void inviting_an_account_whose_email_was_updated_to_mixed_case_still_succeeds() throws Exception {
        String updatePayload = objectMapper.writeValueAsString(
            new UpdateProfileRequest("carol", "Carol@TEST.com"));
        mockMvc.perform(patch("/api/users/me")
                .cookie(accessTokenFor(carolId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatePayload))
            .andExpect(status().isNoContent());

        String invitePayload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.MEMBER));
        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invitePayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("carol@test.com"));
    }

    @Test
    void inviting_an_address_already_a_member_is_refused() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.MEMBER);
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("bob@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict());
    }

    @Test
    void inviting_the_same_address_twice_conflicts_even_with_a_different_casing_then_succeeds_after_revoke()
            throws Exception {
        String firstPayload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.MEMBER));
        String created = mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(firstPayload))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        UUID invitationId = UUID.fromString(objectMapper.readTree(created).get("id").asText());

        // Même adresse, casse différente : la contrainte d'unicité porte sur lower(email).
        String secondPayload = objectMapper.writeValueAsString(new InviteMemberRequest("Carol@test.com", SpaceRole.MEMBER));
        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondPayload))
            .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/invitations/" + invitationId)
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondPayload))
            .andExpect(status().isCreated());
    }

    @Test
    void revoking_an_invitation_from_another_context_is_a_404() throws Exception {
        UUID otherSpaceId = saveSharedSpace("Chez Bob", bobId);
        saveMembership(otherSpaceId, bobId, SpaceRole.OWNER);
        UUID foreignInvitationId = createInvitation(otherSpaceId, "carol@test.com", SpaceRole.MEMBER, bobId);

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/invitations/" + foreignInvitationId)
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void the_owner_role_cannot_be_invited() throws Exception {
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.OWNER));

        mockMvc.perform(post("/api/spaces/" + sharedSpaceId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void the_personal_space_refuses_every_invitation() throws Exception {
        UUID personalId = spaces.findByPersonalOwnerId(aliceId).orElseThrow().getId();
        String payload = objectMapper.writeValueAsString(new InviteMemberRequest("carol@test.com", SpaceRole.MEMBER));

        mockMvc.perform(post("/api/spaces/" + personalId + "/invitations")
                .cookie(accessTokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());
    }

    private UUID createInvitation(UUID spaceId, String email, SpaceRole role, UUID createdBy) {
        SpaceInvitationEntity invitation = new SpaceInvitationEntity();
        invitation.setSpaceId(spaceId);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setCode("NIDO-TEST01");
        invitation.setStatus(com.nido.api.space.domain.model.InvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plusSeconds(3600));
        invitation.setCreatedBy(createdBy);
        return invitations.saveAndFlush(invitation).getId();
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }

    private UUID savePersonalSpace(UUID ownerId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.PERSONAL);
        space.setName("Perso");
        space.setAccent("#8a7d6b");
        space.setGlyph("👤");
        space.setPersonalOwnerId(ownerId);
        space.setCreatedBy(ownerId);
        UUID id = spaces.saveAndFlush(space).getId();
        saveMembership(id, ownerId, SpaceRole.OWNER);
        return id;
    }

    private UUID saveSharedSpace(String name, UUID creatorId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(creatorId);
        return spaces.saveAndFlush(space).getId();
    }

    private void saveMembership(UUID spaceId, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    private Cookie accessTokenFor(UUID userId) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
