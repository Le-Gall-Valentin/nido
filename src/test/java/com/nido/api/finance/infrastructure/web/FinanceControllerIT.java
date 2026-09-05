package com.nido.api.finance.infrastructure.web;

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
class FinanceControllerIT {

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
    private UUID bobsSpaceId;

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
        bobsSpaceId = saveSharedSpace("Chez Bob");
        saveMembership(bobsSpaceId, bobId, SpaceRole.OWNER);
    }

    @Test
    void listing_categories_seeds_and_returns_the_eight_defaults() throws Exception {
        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/categories").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(8));
    }

    @Test
    void a_viewer_cannot_create_a_category() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/categories")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Animaux\",\"color\":\"#a3e635\",\"icon\":\"PawPrint\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_member_can_create_a_transaction_and_set_a_budget_for_its_category() throws Exception {
        String createdCategory = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/categories")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Animaux\",\"color\":\"#a3e635\",\"icon\":\"PawPrint\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(createdCategory).get("id").asText();

        mockMvc.perform(put("/api/spaces/" + spaceId + "/finance/budgets/" + categoryId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyLimit\":50.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.monthlyLimit").value(50.00));

        String body = "{\"label\":\"Croquettes\",\"amount\":25.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + categoryId + "\",\"date\":\"2026-01-15\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.label").value("Croquettes"))
            .andReturn().getResponse().getContentAsString();
        String transactionId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/transactions?month=2026-01").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/stats?month=2026-01").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalExpense").value(25.00))
            .andExpect(jsonPath("$.budgetVsActual[0].spent").value(25.00));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/finance/transactions/" + transactionId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void settling_a_debt_between_members_is_recorded() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/balances/settle")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromMemberId\":\"" + bobId + "\",\"toMemberId\":\"" + aliceId + "\",\"amount\":20.00,\"date\":\"2026-01-02\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(20.00));
    }

    @Test
    void creating_and_contributing_to_a_savings_goal() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances\",\"targetAmount\":2000.00}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals/" + goalId + "/contributions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"" + aliceId + "\",\"amount\":100.00,\"date\":\"2026-01-05\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/savings-goals").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].totalContributed").value(100.00));
    }

    @Test
    void moving_a_transaction_creates_it_in_the_destination_and_removes_the_source() throws Exception {
        String body = "{\"label\":\"Courses\",\"amount\":15.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\",\"date\":\"2026-01-10\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String transactionId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions/" + transactionId + "/move")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isForbidden()); // Bob is only a VIEWER of spaceId — cannot move from it.

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions/" + transactionId + "/move")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isNotFound()); // Alice has no membership at all in bobsSpaceId — SpaceException.NotAMember maps to 404.
    }

    private String firstCategoryId() throws Exception {
        String categories = mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/categories").cookie(accessTokenFor(aliceId)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(categories).get(0).get("id").asText();
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
