package com.nido.api.tasks.infrastructure.web;

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
class RecurringTaskSeriesControllerIT {

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
    void a_member_can_create_list_update_and_delete_a_recurring_task_series() throws Exception {
        String body = "{\"title\":\"Sortir les poubelles\",\"priority\":\"MED\","
            + "\"recurrence\":{\"intervalType\":\"WEEKLY\",\"intervalCount\":1,"
            + "\"leadIntervalType\":\"DAILY\",\"leadIntervalCount\":2,\"anchorDate\":\"2026-01-07\"}}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(created).get("id").asText();
        String seriesListBody = mockMvc.perform(get("/api/spaces/" + spaceId + "/recurring-task-series").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Sortir les poubelles"))
            .andExpect(jsonPath("$[0].createdBy").value(aliceId.toString()))
            .andReturn().getResponse().getContentAsString();
        String seriesId = objectMapper.readTree(seriesListBody).get(0).get("id").asText();

        String updateBody = "{\"title\":\"Sortir les poubelles et le compost\",\"priority\":\"HIGH\",\"subtasks\":[],"
            + "\"recurrence\":{\"intervalType\":\"MONTHLY\",\"intervalCount\":1,"
            + "\"leadIntervalType\":\"WEEKLY\",\"leadIntervalCount\":1,\"anchorDate\":\"2026-01-07\"}}";
        mockMvc.perform(patch("/api/spaces/" + spaceId + "/recurring-task-series/" + seriesId)
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Sortir les poubelles et le compost"))
            .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/recurring-task-series/" + seriesId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/recurring-task-series").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        // The already-materialized first occurrence survives the series deletion, detached.
        mockMvc.perform(get("/api/spaces/" + spaceId + "/tasks").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '" + taskId + "')]").exists());
    }

    @Test
    void a_viewer_cannot_update_or_delete_a_recurring_task_series() throws Exception {
        String updateBody = "{\"title\":\"X\",\"priority\":\"MED\",\"subtasks\":[],"
            + "\"recurrence\":{\"intervalType\":\"WEEKLY\",\"intervalCount\":1,"
            + "\"leadIntervalType\":\"DAILY\",\"leadIntervalCount\":0,\"anchorDate\":\"2026-01-07\"}}";
        mockMvc.perform(patch("/api/spaces/" + spaceId + "/recurring-task-series/" + UUID.randomUUID())
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/spaces/" + spaceId + "/recurring-task-series/" + UUID.randomUUID()).cookie(accessTokenFor(bobId)))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_lead_time_longer_than_the_recurrence_interval_is_rejected_as_a_validation_error() throws Exception {
        String body = "{\"title\":\"Sortir les poubelles\",\"priority\":\"MED\","
            + "\"recurrence\":{\"intervalType\":\"WEEKLY\",\"intervalCount\":1,"
            + "\"leadIntervalType\":\"DAILY\",\"leadIntervalCount\":8,\"anchorDate\":\"2026-01-07\"}}";

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updating_a_nonexistent_recurring_task_series_is_rejected_cleanly_instead_of_crashing() throws Exception {
        String updateBody = "{\"title\":\"X\",\"priority\":\"MED\",\"subtasks\":[],"
            + "\"recurrence\":{\"intervalType\":\"WEEKLY\",\"intervalCount\":1,"
            + "\"leadIntervalType\":\"DAILY\",\"leadIntervalCount\":0,\"anchorDate\":\"2026-01-07\"}}";
        mockMvc.perform(patch("/api/spaces/" + spaceId + "/recurring-task-series/" + UUID.randomUUID())
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(updateBody))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleting_a_nonexistent_recurring_task_series_is_rejected_cleanly_instead_of_crashing() throws Exception {
        mockMvc.perform(delete("/api/spaces/" + spaceId + "/recurring-task-series/" + UUID.randomUUID()).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNotFound());
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
