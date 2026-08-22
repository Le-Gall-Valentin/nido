package com.nido.api.space.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.web.dto.ChangeMemberRoleRequest;
import com.nido.api.space.infrastructure.web.dto.CreateSpaceRequest;
import com.nido.api.space.infrastructure.web.dto.UpdateSpaceRequest;
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
class SpaceControllerIT {

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
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    protected MockMvc mockMvc;
    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected UUID aliceId;
    protected UUID bobId;
    protected UUID sharedSpaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        aliceId = saveUser("alice", Role.USER);
        bobId = saveUser("bob", Role.USER);
        savePersonalSpace(aliceId);
        savePersonalSpace(bobId);
        sharedSpaceId = saveSharedSpace("Chez Valentin", aliceId);
        saveMembership(sharedSpaceId, aliceId, SpaceRole.OWNER);
    }

    @Test
    void listMySpaces_returns_the_personal_space_first() throws Exception {
        mockMvc.perform(get("/api/spaces").cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].type").value("PERSONAL"))
            .andExpect(jsonPath("$[0].myRole").value("OWNER"))
            .andExpect(jsonPath("$[0].memberCount").value(1))
            .andExpect(jsonPath("$[1].name").value("Chez Valentin"));
    }

    @Test
    void listMySpaces_never_leaks_a_space_i_am_not_a_member_of() throws Exception {
        mockMvc.perform(get("/api/spaces").cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("PERSONAL"));
    }

    @Test
    void listMySpaces_without_a_token_is_unauthorized() throws Exception {
        mockMvc.perform(get("/api/spaces"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getSpace_returns_the_detail_for_a_member() throws Exception {
        mockMvc.perform(get("/api/spaces/" + sharedSpaceId).cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Chez Valentin"))
            .andExpect(jsonPath("$.myRole").value("OWNER"))
            .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void getSpace_is_a_404_for_a_non_member() throws Exception {
        mockMvc.perform(get("/api/spaces/" + sharedSpaceId).cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(status().isNotFound());
    }

    @Test
    void getSpace_is_a_404_for_an_unknown_space() throws Exception {
        mockMvc.perform(get("/api/spaces/" + UUID.randomUUID()).cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isNotFound());
    }

    @Test
    void getSpace_is_a_400_for_a_malformed_space_id() throws Exception {
        // Choix assumé : le @PathVariable UUID est converti par Spring avant le résolveur,
        // donc une syntaxe invalide est une erreur de requête. Aucune fuite : une chaîne
        // malformée ne peut désigner aucun contexte, et tout UUID valide rend 404.
        mockMvc.perform(get("/api/spaces/pas-un-uuid").cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void create_makes_the_caller_the_owner() throws Exception {
        String payload = objectMapper.writeValueAsString(
            new CreateSpaceRequest("Chez papa & maman", "La maison familiale", "#4a7fa0", "🏠"));

        mockMvc.perform(post("/api/spaces")
                .cookie(accessTokenFor(bobId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.myRole").value("OWNER"))
            .andExpect(jsonPath("$.type").value("SHARED"));

        mockMvc.perform(get("/api/spaces").cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void create_rejects_an_accent_outside_the_palette() throws Exception {
        String payload = objectMapper.writeValueAsString(
            new CreateSpaceRequest("Chez moi", null, "#123456", "🏠"));

        mockMvc.perform(post("/api/spaces")
                .cookie(accessTokenFor(bobId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void update_is_forbidden_for_a_plain_member() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.MEMBER);
        String payload = objectMapper.writeValueAsString(
            new UpdateSpaceRequest("Renommé", null, "#4a7fa0", "🏠"));

        mockMvc.perform(patch("/api/spaces/" + sharedSpaceId)
                .cookie(accessTokenFor(bobId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());
    }

    @Test
    void the_personal_space_cannot_be_renamed_nor_deleted() throws Exception {
        UUID personalId = spaces.findByPersonalOwnerId(aliceId).orElseThrow().getId();
        String payload = objectMapper.writeValueAsString(
            new UpdateSpaceRequest("Mon perso", null, "#4a7fa0", "🏠"));

        mockMvc.perform(patch("/api/spaces/" + personalId)
                .cookie(accessTokenFor(aliceId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/spaces/" + personalId).cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void delete_is_owner_only() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.ADMIN);

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId).cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId).cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isNoContent());
    }

    @Test
    void listMembers_returns_usernames_and_roles() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.MEMBER);

        mockMvc.perform(get("/api/spaces/" + sharedSpaceId + "/members")
                .cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].username").value("alice"))
            .andExpect(jsonPath("$[0].role").value("OWNER"))
            .andExpect(jsonPath("$[1].username").value("bob"));
    }

    @Test
    void changeMemberRole_is_refused_to_a_viewer_and_accepted_for_an_admin() throws Exception {
        UUID carolId = saveUser("carol", Role.USER);
        savePersonalSpace(carolId);
        saveMembership(sharedSpaceId, bobId, SpaceRole.VIEWER);
        saveMembership(sharedSpaceId, carolId, SpaceRole.MEMBER);
        String payload = objectMapper.writeValueAsString(new ChangeMemberRoleRequest(SpaceRole.VIEWER));

        mockMvc.perform(patch("/api/spaces/" + sharedSpaceId + "/members/" + carolId)
                .cookie(accessTokenFor(bobId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/spaces/" + sharedSpaceId + "/members/" + carolId)
                .cookie(accessTokenFor(aliceId, Role.USER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isNoContent());
    }

    @Test
    void the_owner_membership_cannot_be_touched() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.ADMIN);

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/members/" + aliceId)
                .cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(status().isConflict());
    }

    @Test
    void removeMember_lets_an_admin_remove_a_member() throws Exception {
        saveMembership(sharedSpaceId, bobId, SpaceRole.MEMBER);

        mockMvc.perform(delete("/api/spaces/" + sharedSpaceId + "/members/" + bobId)
                .cookie(accessTokenFor(aliceId, Role.USER)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + sharedSpaceId).cookie(accessTokenFor(bobId, Role.USER)))
            .andExpect(status().isNotFound());
    }

    protected UUID saveUser(String username, Role role) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(role);
        return users.saveAndFlush(user).getId();
    }

    protected UUID savePersonalSpace(UUID ownerId) {
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

    protected UUID saveSharedSpace(String name, UUID creatorId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        space.setCreatedBy(creatorId);
        return spaces.saveAndFlush(space).getId();
    }

    protected void saveMembership(UUID spaceId, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(spaceId);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    protected Cookie accessTokenFor(UUID userId, Role role) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", role.name())
            .claim("email", userId + "@test.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
