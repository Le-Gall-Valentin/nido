package com.nido.api.calendar.infrastructure.web;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarEventControllerIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcTemplate jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID aliceId;
    private UUID bobId;
    private UUID spaceId;
    private UUID bobsSpaceId;

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
        bobsSpaceId = saveSharedSpace("Chez Bob");
        saveMembership(bobsSpaceId, bobId, SpaceRole.OWNER);
    }

    @Test
    void a_member_can_create_read_update_and_delete_an_event() throws Exception {
        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"RDV dentiste","allDay":false,"startDate":"2026-03-02","startTime":"09:00",
                     "endDate":"2026-03-02","endTime":"09:30"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("RDV dentiste"))
            .andExpect(jsonPath("$.createdBy").value(aliceId.toString()))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].source").value("EVENT"))
            .andExpect(jsonPath("$[0].materialized").value(true));

        mockMvc.perform(patch(events() + "/" + eventId)
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"RDV dentiste (reporté)","allDay":true,"startDate":"2026-03-09","endDate":"2026-03-09"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("RDV dentiste (reporté)"))
            .andExpect(jsonPath("$.allDay").value(true));

        mockMvc.perform(delete(events() + "/" + eventId).cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void the_title_is_stored_as_ciphertext() throws Exception {
        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"RDV oncologue","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(objectMapper.readTree(created).get("id").asText());

        String stored = jdbc.queryForObject(
            "SELECT title_encrypted FROM calendar_events WHERE id = ?", String.class, eventId);
        assertThat(stored).doesNotContain("oncologue");
    }

    @Test
    void the_feed_carries_description_and_location_so_an_edit_cannot_erase_them() throws Exception {
        // The edit form and drag-to-reschedule both rebuild the event from the feed. When the feed
        // left these two fields out, opening an event and saving it untouched wiped both.
        mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Concert","description":"Apporter les billets","location":"Salle Pleyel",
                     "allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andExpect(status().isCreated());

        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[0].description").value("Apporter les billets"))
            .andExpect(jsonPath("$[0].location").value("Salle Pleyel"));
    }

    @Test
    void a_viewer_cannot_create_an_event() throws Exception {
        mockMvc.perform(post(events())
                .cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andExpect(status().isForbidden());
    }

    @Test
    void an_outsider_cannot_read_the_calendar() throws Exception {
        UUID carolId = saveUser("carol");
        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(carolId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void an_all_day_event_carrying_times_is_rejected_with_400() throws Exception {
        mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"startDate":"2026-03-02","startTime":"09:00",
                     "endDate":"2026-03-02","endTime":"10:00"}"""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_window_wider_than_a_year_is_rejected_with_400() throws Exception {
        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2027-06-01").cookie(tokenFor(aliceId)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_participant_can_join_and_leave_an_event() throws Exception {
        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Apéro","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post(events() + "/" + eventId + "/participants/me").cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[0].participantIds.length()").value(1));

        mockMvc.perform(delete(events() + "/" + eventId + "/participants/me").cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());
        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[0].participantIds.length()").value(0));
    }

    @Test
    void copying_into_a_space_the_caller_is_not_a_member_of_returns_404() throws Exception {
        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post(events() + "/" + eventId + "/copy")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + bobsSpaceId + "\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    void copying_into_the_same_space_returns_400() throws Exception {
        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02"}"""))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post(events() + "/" + eventId + "/copy")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + spaceId + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_copied_event_arrives_without_its_participants() throws Exception {
        UUID otherSpaceId = saveSharedSpace("Résidence secondaire");
        saveMembership(otherSpaceId, aliceId, SpaceRole.OWNER);

        String created = mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Apéro","allDay":true,"startDate":"2026-03-02","endDate":"2026-03-02",
                     "participantIds":["%s"]}""".formatted(aliceId)))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post(events() + "/" + eventId + "/copy")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationSpaceId\":\"" + otherSpaceId + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Apéro"))
            // Participants belong to the source space's membership; the destination has others.
            .andExpect(jsonPath("$.participantIds.length()").value(0));
    }

    private String events() {
        return "/api/spaces/" + spaceId + "/calendar/events";
    }

    private String occurrences() {
        return "/api/spaces/" + spaceId + "/calendar/occurrences";
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

    private void saveMembership(UUID space, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(space);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    private Cookie tokenFor(UUID userId) {
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
