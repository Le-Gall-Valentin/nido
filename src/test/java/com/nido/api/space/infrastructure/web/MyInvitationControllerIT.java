package com.nido.api.space.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceInvitationEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceInvitationJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import com.nido.api.space.infrastructure.web.dto.AcceptInvitationRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class MyInvitationControllerIT {

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
        carolId = saveUser("carol");
        savePersonalSpace(aliceId);
        savePersonalSpace(carolId);
        sharedSpaceId = saveSharedSpace("Chez Valentin", aliceId);
        saveMembership(sharedSpaceId, aliceId, SpaceRole.OWNER);
    }

    @Test
    void a_pending_invitation_addressed_to_the_caller_appears_in_her_list_with_the_space_name() throws Exception {
        createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST01",
            Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/api/me/invitations").cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].spaceId").value(sharedSpaceId.toString()))
            .andExpect(jsonPath("$[0].spaceName").value("Chez Valentin"))
            .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    void an_invitation_addressed_to_someone_else_does_not_appear_in_the_caller_list() throws Exception {
        createInvitation(sharedSpaceId, "dave@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST02",
            Instant.now().plusSeconds(3600));

        mockMvc.perform(get("/api/me/invitations").cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void accepting_with_the_right_code_creates_the_membership_with_the_invitation_role() throws Exception {
        createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.ADMIN, aliceId, "NIDO-TEST03",
            Instant.now().plusSeconds(3600));
        String payload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST03"));

        mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spaceId").value(sharedSpaceId.toString()));

        mockMvc.perform(get("/api/spaces").cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='" + sharedSpaceId + "')].myRole").value("ADMIN"));
    }

    @Test
    void accepting_by_id_from_the_received_list_creates_the_membership_with_the_invitation_role() throws Exception {
        UUID invitationId = createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.ADMIN, aliceId, "NIDO-TEST09",
            Instant.now().plusSeconds(3600));

        mockMvc.perform(post("/api/invitations/" + invitationId + "/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spaceId").value(sharedSpaceId.toString()));

        mockMvc.perform(get("/api/spaces").cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='" + sharedSpaceId + "')].myRole").value("ADMIN"));
    }

    // Un id inconnu et un id adressé à quelqu'un d'autre rendent exactement le même 404 : c'est
    // ce qui garantit qu'un appelant ne peut pas distinguer, en devinant des identifiants, une
    // invitation inexistante d'une invitation qui ne lui appartient pas. Comparaison champ par
    // champ, corps compris.
    @Test
    void an_unknown_id_and_an_id_addressed_to_someone_else_answer_exactly_alike() throws Exception {
        UUID mismatchedInvitationId = createInvitation(sharedSpaceId, "dave@test.com", SpaceRole.MEMBER, aliceId,
            "NIDO-TEST10", Instant.now().plusSeconds(3600));
        UUID unknownInvitationId = UUID.randomUUID();

        MvcResult mismatchResult = mockMvc.perform(post("/api/invitations/" + mismatchedInvitationId + "/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isNotFound())
            .andReturn();

        MvcResult unknownResult = mockMvc.perform(post("/api/invitations/" + unknownInvitationId + "/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isNotFound())
            .andReturn();

        assertThat(mismatchResult.getResponse().getStatus()).isEqualTo(unknownResult.getResponse().getStatus());
        assertThat(mismatchResult.getResponse().getContentAsString())
            .isEqualTo(unknownResult.getResponse().getContentAsString());
    }

    @Test
    void reusing_the_same_code_is_a_409_single_use() throws Exception {
        createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST04",
            Instant.now().plusSeconds(3600));
        String payload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST04"));

        mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isConflict());
    }

    // Un code destiné à quelqu'un d'autre rend 404, exactement comme un code inconnu : c'est
    // ce qui garantit qu'un appelant ne peut pas moissonner les codes valides en les soumettant
    // au hasard. Comparaison champ par champ, corps compris.
    @Test
    void a_code_addressed_to_someone_else_answers_exactly_like_an_unknown_code() throws Exception {
        createInvitation(sharedSpaceId, "dave@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST05",
            Instant.now().plusSeconds(3600));
        String mismatchPayload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST05"));
        String unknownPayload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-GHOST9"));

        MvcResult mismatchResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mismatchPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        MvcResult unknownResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(unknownPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        assertThat(mismatchResult.getResponse().getStatus()).isEqualTo(unknownResult.getResponse().getStatus());
        assertThat(mismatchResult.getResponse().getContentAsString())
            .isEqualTo(unknownResult.getResponse().getContentAsString());
    }

    // Un code vide, un code blanc ou un code trop long ne doivent rien révéler de plus qu'un
    // code inconnu : aucune annotation de validation ne doit court-circuiter le domaine et
    // produire un 400 distinguable du 404. Comparaison champ par champ, corps compris.
    @Test
    void an_empty_a_blank_and_an_overlong_code_all_answer_exactly_like_an_unknown_code() throws Exception {
        String unknownPayload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-GHOST9"));

        MvcResult unknownResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(unknownPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        String emptyPayload = objectMapper.writeValueAsString(new AcceptInvitationRequest(""));
        MvcResult emptyResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        String blankPayload = objectMapper.writeValueAsString(new AcceptInvitationRequest("   "));
        MvcResult blankResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blankPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        String overlongPayload = objectMapper.writeValueAsString(
            new AcceptInvitationRequest("NIDO-" + "X".repeat(200)));
        MvcResult overlongResult = mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(overlongPayload))
            .andExpect(status().isNotFound())
            .andReturn();

        for (MvcResult result : List.of(emptyResult, blankResult, overlongResult)) {
            assertThat(result.getResponse().getStatus()).isEqualTo(unknownResult.getResponse().getStatus());
            assertThat(result.getResponse().getContentAsString())
                .isEqualTo(unknownResult.getResponse().getContentAsString());
        }
    }

    @Test
    void an_expired_invitation_is_a_422() throws Exception {
        createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST06",
            Instant.now().minusSeconds(60));
        String payload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST06"));

        mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void both_routes_require_authentication() throws Exception {
        String payload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST07"));

        mockMvc.perform(get("/api/me/invitations"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/invitations/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void an_accepted_invitation_no_longer_appears_in_the_received_list() throws Exception {
        createInvitation(sharedSpaceId, "carol@test.com", SpaceRole.MEMBER, aliceId, "NIDO-TEST08",
            Instant.now().plusSeconds(3600));
        String payload = objectMapper.writeValueAsString(new AcceptInvitationRequest("NIDO-TEST08"));

        mockMvc.perform(post("/api/invitations/accept")
                .cookie(accessTokenFor(carolId, "carol@test.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/me/invitations").cookie(accessTokenFor(carolId, "carol@test.com")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    private UUID createInvitation(UUID spaceId, String email, SpaceRole role, UUID createdBy, String code, Instant expiresAt) {
        SpaceInvitationEntity invitation = new SpaceInvitationEntity();
        invitation.setSpaceId(spaceId);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setCode(code);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(expiresAt);
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

    private Cookie accessTokenFor(UUID userId, String email) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", email)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
