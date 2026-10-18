package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
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
import org.junit.jupiter.api.AfterEach;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarImportControllerIT {

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

    // As Google writes it, year-0 CREATED included: a value nido never reads must not refuse the file.
    private static final String GOOGLE = """
        BEGIN:VCALENDAR\r
        PRODID:-//Google Inc//Google Calendar 70.9054//EN\r
        VERSION:2.0\r
        X-WR-CALNAME:Club\r
        BEGIN:VEVENT\r
        DTSTART:20261014T160000Z\r
        DTEND:20261014T170000Z\r
        DTSTAMP:20261013T120000Z\r
        UID:7kukuqrfedlm2f9t7vr5a2g0ok@google.com\r
        CREATED:00001231T000000Z\r
        LAST-MODIFIED:20261001T080000Z\r
        SEQUENCE:0\r
        SUMMARY:Entraînement\r
        DESCRIPTION:<b>Apporter</b> les maillots\r
        END:VEVENT\r
        BEGIN:VEVENT\r
        DTSTART;VALUE=DATE:20260105\r
        DTSTAMP:20261013T120000Z\r
        UID:9xyz@google.com\r
        SUMMARY:Piscine\r
        RRULE:FREQ=WEEKLY;BYDAY=MO,TH\r
        END:VEVENT\r
        END:VCALENDAR\r
        """;

    @AfterEach
    void cleanUp() {
        members.deleteAll();
        spaces.deleteAll();
        users.deleteAll();
    }

    @Test
    void the_analysis_writes_nothing() throws Exception {
        mockMvc.perform(post(imports() + "/analysis").cookie(tokenFor(aliceId)).contentType("text/calendar").content(GOOGLE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventsCreated").value(1))
            .andExpect(jsonPath("$.seriesCreated").value(1))
            .andExpect(jsonPath("$.written").value(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_events WHERE space_id = ?", Long.class, spaceId)).isZero();
    }

    @Test
    void importing_the_same_file_twice_creates_nothing_the_second_time() throws Exception {
        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(GOOGLE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.written").value(true))
            .andExpect(jsonPath("$.eventsCreated").value(1))
            .andExpect(jsonPath("$.seriesCreated").value(1));

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(GOOGLE))
            .andExpect(jsonPath("$.eventsCreated").value(0))
            .andExpect(jsonPath("$.seriesCreated").value(0))
            .andExpect(jsonPath("$.duplicates").value(2));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/calendar/occurrences?from=2026-10-12&to=2026-10-18").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$[?(@.title == 'Entraînement')].startTime").value("18:00:00"))
            .andExpect(jsonPath("$[?(@.title == 'Piscine')]", hasSize(2)));
    }

    @Test
    void a_nido_export_comes_back_as_a_copy_in_another_space_only() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/events").cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Concert\",\"allDay\":true,\"startDate\":\"2026-09-23\",\"endDate\":\"2026-09-23\"}"))
            .andExpect(status().isCreated());
        String export = mockMvc.perform(get("/api/spaces/" + spaceId + "/calendar/export.ics?sources=EVENT").cookie(tokenFor(aliceId)))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(export))
            .andExpect(jsonPath("$.duplicates").value(1));
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/calendar/import").cookie(tokenFor(bobId))
                .contentType("text/calendar").content(export))
            .andExpect(jsonPath("$.eventsCreated").value(1));
    }

    @Test
    void a_viewer_cannot_import() throws Exception {
        // Refused by the space before the calendar is asked: the space's 403 names the role, with no error_code of its own.
        mockMvc.perform(post(imports() + "/analysis").cookie(tokenFor(bobId)).contentType("text/calendar").content(GOOGLE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("InsufficientRole"));
        mockMvc.perform(post(imports()).cookie(tokenFor(bobId)).contentType("text/calendar").content(GOOGLE))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.title").value("InsufficientRole"));
    }

    @Test
    void a_slot_edited_twice_in_the_file_is_imported_once_in_its_latest_version() throws Exception {
        String twice = """
            BEGIN:VCALENDAR\r
            VERSION:2.0\r
            PRODID:-//test//EN\r
            BEGIN:VEVENT\r
            UID:dup@test\r
            DTSTAMP:20261013T120000Z\r
            SUMMARY:Piano\r
            DTSTART;VALUE=DATE:20260105\r
            RRULE:FREQ=WEEKLY;BYDAY=MO\r
            END:VEVENT\r
            BEGIN:VEVENT\r
            UID:dup@test\r
            DTSTAMP:20261013T120000Z\r
            SEQUENCE:2\r
            SUMMARY:Piano (salle 3)\r
            RECURRENCE-ID;VALUE=DATE:20260112\r
            DTSTART;VALUE=DATE:20260114\r
            END:VEVENT\r
            BEGIN:VEVENT\r
            UID:dup@test\r
            DTSTAMP:20261013T120000Z\r
            SEQUENCE:1\r
            SUMMARY:Piano (salle 2)\r
            RECURRENCE-ID;VALUE=DATE:20260112\r
            DTSTART;VALUE=DATE:20260113\r
            END:VEVENT\r
            END:VCALENDAR\r
            """;

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(twice))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.written").value(true))
            .andExpect(jsonPath("$.seriesCreated").value(1))
            .andExpect(jsonPath("$.ignored").value(1))
            .andExpect(jsonPath("$.notes[0].title").value("Piano (salle 2)"))
            .andExpect(jsonPath("$.notes[0].detail").value("DUPLICATE_UID"));

        mockMvc.perform(get("/api/spaces/" + spaceId + "/calendar/occurrences?from=2026-01-12&to=2026-01-18").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title").value("Piano (salle 3)"))
            .andExpect(jsonPath("$[0].startDate").value("2026-01-14"));
    }

    @Test
    void the_notes_of_the_reading_reach_the_answer() throws Exception {
        String noted = """
            BEGIN:VCALENDAR\r
            VERSION:2.0\r
            PRODID:-//test//EN\r
            BEGIN:VTODO\r
            UID:todo@test\r
            DTSTAMP:20261013T120000Z\r
            SUMMARY:Acheter du pain\r
            END:VTODO\r
            BEGIN:VEVENT\r
            UID:task@nido\r
            DTSTAMP:20261013T120000Z\r
            SUMMARY:Ordinaire\r
            DTSTART;VALUE=DATE:20261017\r
            END:VEVENT\r
            BEGIN:VEVENT\r
            UID:TASK-5d0f6a0e-8f7c-4b8e-9a51-2f0c2b8a1d11@nido\r
            DTSTAMP:20261013T120000Z\r
            SUMMARY:Tâche · Poubelles\r
            DTSTART;VALUE=DATE:20261017\r
            END:VEVENT\r
            END:VCALENDAR\r
            """;

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(noted))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.eventsCreated").value(1))
            .andExpect(jsonPath("$.ignored").value(2))
            .andExpect(jsonPath("$.notes", hasSize(2)))
            .andExpect(jsonPath("$.notes[0].kind").value("IGNORED"))
            .andExpect(jsonPath("$.notes[0].title").value("Acheter du pain"))
            .andExpect(jsonPath("$.notes[0].detail").value("VTODO"))
            .andExpect(jsonPath("$.notes[1].title").value("Tâche · Poubelles"))
            .andExpect(jsonPath("$.notes[1].detail").value("NIDO_SOURCE"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_events WHERE space_id = ?", Long.class, spaceId)).isEqualTo(1);
    }

    @Test
    void more_events_than_an_import_takes_is_refused_and_writes_nothing() throws Exception {
        StringBuilder many = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//test//EN\r\n");
        for (int i = 0; i <= 5_000; i++) {
            many.append("BEGIN:VEVENT\r\nUID:e").append(i).append("@test\r\nDTSTAMP:20261013T120000Z\r\n")
                .append("DTSTART;VALUE=DATE:20261017\r\nEND:VEVENT\r\n");
        }
        many.append("END:VCALENDAR\r\n");

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(many.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("import_too_large"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_events WHERE space_id = ?", Long.class, spaceId)).isZero();
    }

    @Test
    void a_nido_export_of_a_series_comes_back_whole_in_another_space_and_as_nothing_in_its_own() throws Exception {
        String created = mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/recurring-event-series")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Marché","allDay":true,"intervalType":"WEEKLY","intervalCount":1,
                     "weekdays":["SATURDAY"],"anchorDate":"2026-01-03","endDate":"2026-03-28"}"""))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String seriesPath = "/api/spaces/" + spaceId + "/calendar/recurring-event-series/" + objectMapper.readTree(created).get("id").asText();
        mockMvc.perform(delete(seriesPath + "/occurrences/2026-01-10")
                .cookie(tokenFor(aliceId)))
            .andExpect(status().isNoContent());
        mockMvc.perform(put(seriesPath + "/occurrences/2026-01-17")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Marché (place)\",\"allDay\":true,\"startDate\":\"2026-01-18\",\"endDate\":\"2026-01-18\"}"))
            .andExpect(status().isOk());
        String export = mockMvc.perform(get("/api/spaces/" + spaceId + "/calendar/export.ics?sources=EVENT").cookie(tokenFor(aliceId)))
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType("text/calendar").content(export))
            .andExpect(jsonPath("$.seriesCreated").value(0))
            .andExpect(jsonPath("$.eventsCreated").value(0))
            .andExpect(jsonPath("$.duplicates").value(1));
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/calendar/import").cookie(tokenFor(bobId))
                .contentType("text/calendar").content(export))
            .andExpect(jsonPath("$.seriesCreated").value(1))
            .andExpect(jsonPath("$.seriesUnfolded").value(0));

        String shown = mockMvc.perform(get("/api/spaces/" + bobsSpaceId + "/calendar/occurrences?from=2026-01-01&to=2026-01-31")
                .cookie(tokenFor(bobId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> days = new ArrayList<>();
        for (JsonNode occurrence : objectMapper.readTree(shown)) {
            days.add(occurrence.get("startDate").asText() + " " + occurrence.get("title").asText()
                + (occurrence.get("allDay").asBoolean() ? "" : " timed"));
        }
        assertThat(days).containsExactly("2026-01-03 Marché", "2026-01-18 Marché (place)", "2026-01-24 Marché", "2026-01-31 Marché");
    }

    @Test
    void what_is_not_a_calendar_is_a_400_and_what_is_too_big_a_413() throws Exception {
        mockMvc.perform(post(imports() + "/analysis").cookie(tokenFor(aliceId)).contentType("text/calendar")
                .content("<html>Not found</html>"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("not_ics"));
        mockMvc.perform(post(imports() + "/analysis").cookie(tokenFor(aliceId)).contentType("text/calendar")
                .content(new byte[5 * 1024 * 1024 + 1]))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(jsonPath("$.error_code").value("file_too_large"));
    }

    @Test
    void a_form_post_is_not_taken() throws Exception {
        // Only text/calendar: a type a page of another site cannot send without the browser asking first.
        mockMvc.perform(post(imports()).cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_FORM_URLENCODED).content("x=1"))
            .andExpect(status().isUnsupportedMediaType());
    }

    private String imports() {
        return "/api/spaces/" + spaceId + "/calendar/import";
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
        TestSpaces.name(space, name);
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
