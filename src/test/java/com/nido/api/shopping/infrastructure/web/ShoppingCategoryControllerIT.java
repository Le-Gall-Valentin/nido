package com.nido.api.shopping.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.entity.SpaceMemberEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class ShoppingCategoryControllerIT {

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

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID aliceId;
    private UUID bobId;
    private UUID spaceId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
        rateLimitBucketStore.clearAll();
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        spaceId = saveSharedSpace("Chez Valentin");
        saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        saveMembership(spaceId, bobId, SpaceRole.VIEWER);
    }

    @Test
    void listing_categories_the_first_time_seeds_and_returns_the_defaults() throws Exception {
        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/categories").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8))
            .andExpect(jsonPath("$[7].name").value("Maison & divers"))
            .andExpect(jsonPath("$[7].fallback").value(true));
    }

    @Test
    void a_member_can_create_rename_and_delete_a_category() throws Exception {
        // Listing first triggers the lazy default-category seed (including the
        // fallback category), matching the real frontend flow — without it, this
        // space would have no fallback category yet to reassign the deleted
        // category's items to, and delete would 404.
        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/categories").cookie(accessTokenFor(aliceId)));

        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/categories")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bricolage\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/shopping/categories/" + categoryId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bricolage & jardin\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Bricolage & jardin"));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/shopping/categories/" + categoryId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void the_fallback_category_cannot_be_deleted() throws Exception {
        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/categories").cookie(accessTokenFor(aliceId)));
        String all = mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/categories").cookie(accessTokenFor(aliceId)))
            .andReturn().getResponse().getContentAsString();
        String fallbackId = objectMapper.readTree(all).get(7).get("id").asText();

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/shopping/categories/" + fallbackId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_viewer_cannot_create_a_category() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/categories")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bricolage\"}"))
            .andExpect(status().isForbidden());
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        return users.saveAndFlush(user).getId();
    }

    private UUID saveSharedSpace(String name) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName(name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
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
