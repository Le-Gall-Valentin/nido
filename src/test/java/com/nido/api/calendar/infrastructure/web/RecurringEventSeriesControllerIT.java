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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class RecurringEventSeriesControllerIT {

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
    void every_slot_of_a_weekly_series_shows_in_the_window() throws Exception {
        createWeeklySeries();

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-06"))
            .andExpect(jsonPath("$[0].materialized").value(false))
            .andExpect(jsonPath("$[3].startDate").value("2026-01-27"));
    }

    @Test
    void cancelling_one_occurrence_removes_exactly_that_slot() throws Exception {
        String seriesId = createWeeklySeries();

        mockMvc.perform(delete(series() + "/" + seriesId + "/occurrences/2026-01-13").cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-06"))
            .andExpect(jsonPath("$[1].startDate").value("2026-01-20"));
    }

    @Test
    void editing_one_occurrence_moves_it_without_touching_its_neighbours() throws Exception {
        String seriesId = createWeeklySeries();

        mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano (décalé)","allDay":false,"startDate":"2026-01-15","startTime":"19:00",
                     "endDate":"2026-01-15","endTime":"20:00"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recurringOriginalDate").value("2026-01-13"));

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-06"))
            // The 13th is gone, the 15th has taken its place, and the 20th/27th are untouched.
            .andExpect(jsonPath("$[1].startDate").value("2026-01-15"))
            .andExpect(jsonPath("$[1].title").value("Piano (décalé)"))
            .andExpect(jsonPath("$[1].materialized").value(true))
            .andExpect(jsonPath("$[2].startDate").value("2026-01-20"));
    }

    @Test
    void deleting_an_edited_occurrence_leaves_the_slot_gone_rather_than_restoring_it() throws Exception {
        String seriesId = createWeeklySeries();
        String detached = mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano (décalé)","allDay":true,"startDate":"2026-01-15","endDate":"2026-01-15"}"""))
            .andReturn().getResponse().getContentAsString();
        String eventId = objectMapper.readTree(detached).get("id").asText();

        mockMvc.perform(delete(events() + "/" + eventId).cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());

        // Without the exclusion written on delete, the 13th would come straight back.
        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-06"))
            .andExpect(jsonPath("$[1].startDate").value("2026-01-20"));
    }

    @Test
    void reinstating_a_cancelled_occurrence_by_editing_it_makes_it_visible_again() throws Exception {
        String seriesId = createWeeklySeries();
        mockMvc.perform(delete(series() + "/" + seriesId + "/occurrences/2026-01-13").cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano (finalement)","allDay":true,"startDate":"2026-01-13","endDate":"2026-01-13"}"""))
            .andExpect(status().isOk());

        // The exclusion must have been lifted, or the new event would exist but stay hidden.
        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[1].title").value("Piano (finalement)"));
    }

    @Test
    void editing_a_date_the_series_never_produces_returns_400() throws Exception {
        String seriesId = createWeeklySeries();

        // 2026-01-08 is a Thursday; the series runs on Tuesdays.
        mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-08")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"startDate":"2026-01-08","endDate":"2026-01-08"}"""))
            .andExpect(status().isBadRequest());
    }

    @Test
    void the_put_is_idempotent() throws Exception {
        String seriesId = createWeeklySeries();
        String body = """
            {"title":"Piano (décalé)","allDay":true,"startDate":"2026-01-15","endDate":"2026-01-15"}""";

        String first = mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Same row, not a second event: replaying the request yields the same state.
        assertThat(objectMapper.readTree(second).get("id").asText())
            .isEqualTo(objectMapper.readTree(first).get("id").asText());
        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void deleting_a_series_cascades_to_its_exclusions_and_detached_instances() throws Exception {
        String seriesId = createWeeklySeries();
        mockMvc.perform(delete(series() + "/" + seriesId + "/occurrences/2026-01-06").cookie(tokenFor(aliceId)));
        mockMvc.perform(put(series() + "/" + seriesId + "/occurrences/2026-01-13")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano (décalé)","allDay":true,"startDate":"2026-01-15","endDate":"2026-01-15"}"""));

        mockMvc.perform(delete(series() + "/" + seriesId).cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());

        UUID id = UUID.fromString(seriesId);
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM calendar_event_exclusions WHERE series_id = ?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject(
            "SELECT count(*) FROM calendar_events WHERE recurring_series_id = ?", Integer.class, id)).isZero();
        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void a_multi_day_recurring_series_spans_its_duration() throws Exception {
        mockMvc.perform(post(series())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Week-end","allDay":true,"durationDays":2,"intervalType":"MONTHLY",
                     "intervalCount":1,"anchorDate":"2026-01-10"}"""))
            .andExpect(status().isCreated());

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-10"))
            .andExpect(jsonPath("$[0].endDate").value("2026-01-12"));
    }

    @Test
    void an_occurrence_lasting_longer_than_its_interval_is_refused() throws Exception {
        // A daily series whose occurrences last a million days projected 739,901 of them into every
        // read of a single month.
        mockMvc.perform(post(series())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Sans fin","allDay":true,"durationDays":1000000,"intervalType":"DAILY",
                     "intervalCount":1,"anchorDate":"0001-01-01"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("OccurrenceLongerThanInterval"));
    }

    @Test
    void a_participant_who_left_the_space_does_not_block_editing_the_series() throws Exception {
        UUID carolId = saveUser("carol");
        saveMembership(spaceId, carolId, SpaceRole.MEMBER);
        String body = """
            {"title":"%s","allDay":true,"intervalType":"WEEKLY","intervalCount":1,"anchorDate":"2026-01-06",
             "participantIds":["%s"]}""";
        String created = mockMvc.perform(post(series()).cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted("Piano", carolId)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String seriesId = objectMapper.readTree(created).get("id").asText();
        jdbc.update("DELETE FROM space_members WHERE space_id = ? AND user_id = ?", spaceId, carolId);

        mockMvc.perform(patch(series() + "/" + seriesId).cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content(body.formatted("Piano (salle 3)", carolId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.participantIds[0]").value(carolId.toString()));
    }

    @Test
    void a_viewer_cannot_create_a_series() throws Exception {
        mockMvc.perform(post(series())
                .cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"intervalType":"WEEKLY","intervalCount":1,"anchorDate":"2026-01-06"}"""))
            .andExpect(status().isForbidden());
    }

    @Test
    void a_same_day_series_needs_no_durationDays_and_an_omitted_allDay_means_timed() throws Exception {
        // Jackson refuses to map an absent JSON field onto a primitive record component, so both of
        // these are boxed and defaulted. Without that, omitting either returned an opaque
        // "Failed to read request" 400 instead of the documented default.
        mockMvc.perform(post(series())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano","startTime":"18:00","endTime":"19:00",
                     "intervalType":"WEEKLY","intervalCount":1,"anchorDate":"2026-01-06"}"""))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.allDay").value(false))
            .andExpect(jsonPath("$.durationDays").value(0));

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-01-07").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-06"))
            .andExpect(jsonPath("$[0].endDate").value("2026-01-06"));
    }

    @Test
    void a_series_ending_before_its_anchor_is_rejected_with_400() throws Exception {
        mockMvc.perform(post(series())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"x","allDay":true,"intervalType":"WEEKLY","intervalCount":1,
                     "anchorDate":"2026-02-01","endDate":"2026-01-01"}"""))
            .andExpect(status().isBadRequest());
    }

    /** Weekly, Tuesdays, from 2026-01-06. */
    private String createWeeklySeries() throws Exception {
        String created = mockMvc.perform(post(series())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano","allDay":false,"startTime":"18:00","endTime":"19:00","durationDays":0,
                     "intervalType":"WEEKLY","intervalCount":1,"anchorDate":"2026-01-06"}"""))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asText();
    }

    private String series() {
        return "/api/spaces/" + spaceId + "/calendar/recurring-event-series";
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
