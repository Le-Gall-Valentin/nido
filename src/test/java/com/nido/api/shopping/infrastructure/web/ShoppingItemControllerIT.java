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
class ShoppingItemControllerIT {

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
    private UUID categoryId;

    @BeforeEach
    void setUp() throws Exception {
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

        String categories = mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/categories").cookie(accessTokenFor(aliceId)))
            .andReturn().getResponse().getContentAsString();
        categoryId = UUID.fromString(objectMapper.readTree(categories).get(0).get("id").asText());
    }

    @Test
    void a_member_can_add_toggle_and_delete_an_item() throws Exception {
        // jsonPath(...).value(500) relies on Jackson serializing the BigDecimal quantity as a
        // bare JSON number rather than a string — if that serialization ever changes, this
        // assertion (and the others like it below) would need to switch to value("500").
        String body = "{\"categoryId\":\"" + categoryId + "\",\"name\":\"Pâtes\",\"quantity\":500,\"unit\":\"GRAM\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pâtes"))
            .andExpect(jsonPath("$.quantity").value(500))
            .andExpect(jsonPath("$.unit").value("GRAM"))
            .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/shopping/items/" + itemId + "/done").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/items").cookie(accessTokenFor(aliceId)))
            .andExpect(jsonPath("$[0].done").value(true));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/shopping/items/" + itemId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void clear_done_only_removes_done_items() throws Exception {
        String pendingBody = "{\"categoryId\":\"" + categoryId + "\",\"name\":\"Riz\"}";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items")
            .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(pendingBody));
        String doneBody = "{\"categoryId\":\"" + categoryId + "\",\"name\":\"Pâtes\"}";
        String done = mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(doneBody))
            .andReturn().getResponse().getContentAsString();
        String doneId = objectMapper.readTree(done).get("id").asText();
        mockMvc.perform(patch("/api/spaces/" + spaceId + "/shopping/items/" + doneId + "/done").cookie(accessTokenFor(aliceId)));

        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items/clear-done").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/items").cookie(accessTokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Riz"));
    }

    @Test
    void importing_from_menu_upserts_by_normalized_name_and_never_touches_done_items() throws Exception {
        String firstImport = "{\"lines\":[{\"name\":\"Poulet\",\"quantity\":800,\"unit\":\"GRAM\",\"categoryId\":\"" + categoryId + "\"}]}";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items/import-from-menu")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(firstImport))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quantity").value(800))
            .andExpect(jsonPath("$[0].unit").value("GRAM"));

        String secondImport = "{\"lines\":[{\"name\":\"Poulets\",\"quantity\":1,\"unit\":\"KILOGRAM\",\"categoryId\":\"" + categoryId + "\"}]}";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items/import-from-menu")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(secondImport))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/shopping/items").cookie(accessTokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].quantity").value(1))
            .andExpect(jsonPath("$[0].unit").value("KILOGRAM"));
    }

    @Test
    void a_viewer_cannot_add_an_item() throws Exception {
        String body = "{\"categoryId\":\"" + categoryId + "\",\"name\":\"Pâtes\"}";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/shopping/items")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(body))
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
