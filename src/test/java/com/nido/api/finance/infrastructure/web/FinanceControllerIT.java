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
    void a_third_member_cannot_settle_a_debt_between_two_others() throws Exception {
        UUID carolId = saveUser("carol");
        saveMembership(spaceId, carolId, SpaceRole.ADMIN);

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/balances/settle")
                .cookie(accessTokenFor(carolId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromMemberId\":\"" + bobId + "\",\"toMemberId\":\"" + aliceId + "\",\"amount\":20.00,\"date\":\"2026-01-02\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void listing_settlements_between_two_members_returns_them_newest_first() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/balances/settle")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromMemberId\":\"" + bobId + "\",\"toMemberId\":\"" + aliceId + "\",\"amount\":20.00,\"date\":\"2026-01-02\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/balances/settle")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"fromMemberId\":\"" + bobId + "\",\"toMemberId\":\"" + aliceId + "\",\"amount\":15.00,\"date\":\"2026-02-01\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/balances/settlements")
                .param("memberAId", aliceId.toString()).param("memberBId", bobId.toString())
                .cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].amount").value(15.00))
            .andExpect(jsonPath("$[1].amount").value(20.00));
    }

    @Test
    void a_member_can_create_list_update_and_delete_a_recurring_series() throws Exception {
        String body = "{\"label\":\"Loyer\",\"amount\":800.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\","
            + "\"recurrence\":{\"intervalType\":\"MONTHLY\",\"intervalCount\":1,\"anchorDate\":\"2026-01-01\"}}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/recurring-series")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.label").value("Loyer"))
            .andReturn().getResponse().getContentAsString();
        String seriesId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/recurring-series").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        String updateBody = "{\"label\":\"Loyer révisé\",\"amount\":850.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\","
            + "\"recurrence\":{\"intervalType\":\"MONTHLY\",\"intervalCount\":1,\"anchorDate\":\"2026-01-01\"}}";
        mockMvc.perform(patch("/api/spaces/" + spaceId + "/finance/recurring-series/" + seriesId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.label").value("Loyer révisé"))
            .andExpect(jsonPath("$.amount").value(850.00));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/finance/recurring-series/" + seriesId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/recurring-series").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void a_viewer_cannot_create_a_recurring_series() throws Exception {
        String body = "{\"label\":\"Loyer\",\"amount\":800.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\","
            + "\"recurrence\":{\"intervalType\":\"MONTHLY\",\"intervalCount\":1,\"anchorDate\":\"2026-01-01\"}}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/recurring-series")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_recurring_series_interval_count_of_zero_is_rejected_as_a_validation_error() throws Exception {
        String body = "{\"label\":\"Loyer\",\"amount\":800.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\","
            + "\"recurrence\":{\"intervalType\":\"MONTHLY\",\"intervalCount\":0,\"anchorDate\":\"2026-01-01\"}}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/recurring-series")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deleting_a_nonexistent_recurring_series_is_rejected_cleanly_instead_of_crashing() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId + "/finance/recurring-series/" + UUID.randomUUID()).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void creating_and_contributing_to_a_savings_goal() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances\",\"targetAmount\":2000.00,\"color\":\"#5c7a58\",\"glyph\":\"🎯\"}"))
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
    void updating_a_savings_goal_still_reports_its_existing_contributions_instead_of_resetting_them_to_zero() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances\",\"targetAmount\":2000.00,\"color\":\"#5c7a58\",\"glyph\":\"🎯\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String goalId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals/" + goalId + "/contributions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"memberId\":\"" + aliceId + "\",\"amount\":100.00,\"date\":\"2026-01-05\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/spaces/" + spaceId + "/finance/savings-goals/" + goalId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances d'été\",\"targetAmount\":2500.00,\"color\":\"#5c7a58\",\"glyph\":\"🎯\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalContributed").value(100.00))
            .andExpect(jsonPath("$.contributions.length()").value(1));
    }

    @Test
    void an_amount_with_more_than_two_decimal_places_is_rejected_as_a_validation_error() throws Exception {
        String body = "{\"label\":\"Courses\",\"amount\":12.345678,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\",\"date\":\"2026-01-10\","
            + "\"contributors\":[{\"memberId\":\"" + aliceId + "\",\"shareAmount\":null}]}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_negative_contributor_share_amount_is_rejected_as_a_validation_error() throws Exception {
        // Shares still sum to the total (-5.00 + 25.00 = 20.00) so this is rejected by
        // bean validation on shareAmount itself, not by the shares-sum-mismatch business rule.
        String body = "{\"label\":\"Courses\",\"amount\":20.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + firstCategoryId() + "\",\"date\":\"2026-01-10\",\"payerId\":\"" + aliceId + "\","
            + "\"contributors\":[{\"memberId\":\"" + aliceId + "\",\"shareAmount\":-5.00},{\"memberId\":\"" + bobId + "\",\"shareAmount\":25.00}]}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void creating_a_transaction_with_a_nonexistent_category_is_rejected_cleanly_instead_of_crashing() throws Exception {
        String body = "{\"label\":\"Courses\",\"amount\":25.00,\"type\":\"EXPENSE\",\"categoryId\":\"" + UUID.randomUUID() + "\",\"date\":\"2026-01-10\"}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/transactions")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound());
    }

    @Test
    void a_budget_amount_with_more_than_two_decimal_places_is_rejected_as_a_validation_error() throws Exception {
        mockMvc.perform(put("/api/spaces/" + spaceId + "/finance/budgets/" + firstCategoryId())
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyLimit\":50.001}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_savings_goal_target_amount_with_more_than_two_decimal_places_is_rejected_as_a_validation_error() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vacances\",\"targetAmount\":2000.999,\"color\":\"#5c7a58\",\"glyph\":\"🎯\"}"))
            .andExpect(status().isBadRequest());
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
