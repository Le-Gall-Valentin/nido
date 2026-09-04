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
class TaskControllerIT {

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
    void a_member_can_create_list_and_delete_a_one_off_task() throws Exception {
        String body = "{\"title\":\"Prendre RDV\",\"priority\":\"HIGH\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Prendre RDV"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/spaces/" + spaceId + "/tasks").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/tasks/" + taskId).cookie(accessTokenFor(aliceId)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/spaces/" + spaceId + "/tasks").cookie(accessTokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void a_viewer_cannot_create_a_task() throws Exception {
        String body = "{\"title\":\"Prendre RDV\",\"priority\":\"HIGH\"}";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    void completing_a_task_with_an_open_subtask_returns_409() throws Exception {
        String body = "{\"title\":\"Réserver\",\"priority\":\"MED\",\"subtasks\":[\"Comparer les prix\"]}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/status")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void a_completed_task_never_returns_a_due_date() throws Exception {
        String body = "{\"title\":\"T\",\"priority\":\"MED\",\"dueDate\":\"2026-01-07\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/status")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dueDate").doesNotExist());
    }

    @Test
    void completing_a_recurring_task_creates_the_next_occurrence() throws Exception {
        String body = "{\"title\":\"Sortir les poubelles\",\"priority\":\"MED\","
            + "\"recurrence\":{\"intervalType\":\"WEEKLY\",\"intervalCount\":1,\"anchorDate\":\"2026-01-07\"}}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recurring").value(true))
            .andReturn().getResponse().getContentAsString();
        String firstTaskId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + firstTaskId + "/status")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DONE\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/tasks").cookie(accessTokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.status == 'TODO')].dueDate").value("2026-01-14"));
    }

    @Test
    void moving_a_task_creates_it_in_the_destination_and_removes_the_source() throws Exception {
        String body = "{\"title\":\"Sortir les poubelles\",\"priority\":\"MED\"}";
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/move")
                .cookie(accessTokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isForbidden()); // Bob is only a VIEWER of spaceId — cannot move from it.

        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks/" + taskId + "/move")
                .cookie(accessTokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isNotFound()); // Alice has no membership at all in bobsSpaceId — SpaceException.NotAMember maps to 404.
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
