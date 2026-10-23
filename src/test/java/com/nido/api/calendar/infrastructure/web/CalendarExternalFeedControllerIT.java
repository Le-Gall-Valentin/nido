package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestAccessTokens;
import com.nido.api.TestPeople;
import com.nido.api.calendar.domain.model.FeedDownload;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.model.FeedErrorKind;
import com.nido.api.calendar.domain.port.out.ExternalFeedDownloader;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarExternalFeedControllerIT {

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcTemplate jdbc;
    @Autowired com.nido.api.calendar.domain.port.out.ExternalFeedRepository externalFeeds;

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

        aliceId = TestPeople.user(users, "alice");
        bobId = TestPeople.user(users, "bob");
        spaceId = TestPeople.sharedSpace(spaces, "Chez Valentin");
        TestPeople.member(members, spaceId, aliceId, SpaceRole.OWNER);
        TestPeople.member(members, spaceId, bobId, SpaceRole.VIEWER);
        bobsSpaceId = TestPeople.sharedSpace(spaces, "Chez Bob");
        TestPeople.member(members, bobsSpaceId, bobId, SpaceRole.OWNER);
    }

    @MockitoBean ExternalFeedDownloader downloader;

    private static final String SCHOOL = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\nX-WR-CALNAME:Vacances scolaires\r\n"
        + "BEGIN:VEVENT\r\nUID:toussaint@ecole\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Vacances de la Toussaint\r\n"
        + "DTSTART;VALUE=DATE:20261017\r\nDTEND;VALUE=DATE:20261102\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM calendar_external_feeds WHERE space_id IN (?, ?)", spaceId, bobsSpaceId);
    }

    @Test
    void a_member_follows_a_calendar_every_member_sees_without_its_address() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, "\"v1\"", null));

        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"webcal://ecole.example/vacances.ics?token=secret\",\"color\":\"event-cyan\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Vacances scolaires"))
            .andExpect(jsonPath("$.host").value("ecole.example"))
            .andExpect(jsonPath("$.url").doesNotExist());

        mockMvc.perform(get(feeds()).cookie(TestAccessTokens.cookieFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("Vacances scolaires"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret"))));
    }

    @Test
    void a_viewer_cannot_follow_a_calendar() throws Exception {
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andExpect(status().isForbidden());
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void an_address_that_fails_says_why() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Failed(FeedError.http(404)));
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("feed_unavailable"))
            .andExpect(jsonPath("$.feed_error").value("HTTP_404"));
    }

    @Test
    void a_calendar_is_renamed_refreshed_and_removed() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, "\"v1\"", null));
        String id = objectMapper.readTree(mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch(feeds() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"École\",\"color\":\"event-indigo\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("École"));

        // Asked again within the minute: refused, without calling the address.
        mockMvc.perform(post(feeds() + "/" + id + "/refresh").cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error_code").value("refresh_too_soon"));

        jdbc.update("UPDATE calendar_external_feeds SET last_attempt_at = now() - interval '2 minutes' WHERE space_id = ?", spaceId);
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Failed(FeedError.of(FeedErrorKind.TIMEOUT)));
        mockMvc.perform(post(feeds() + "/" + id + "/refresh").cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastError").value("TIMEOUT"));

        mockMvc.perform(delete(feeds() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId))).andExpect(status().isNoContent());
        mockMvc.perform(get(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void a_member_of_another_space_cannot_rename_refresh_or_remove_this_spaces_calendar() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, "\"v1\"", null));
        String id = objectMapper.readTree(mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andReturn().getResponse().getContentAsString()).get("id").asText();
        jdbc.update("UPDATE calendar_external_feeds SET last_attempt_at = now() - interval '2 minutes' WHERE space_id = ?", spaceId);
        Instant attemptBefore = jdbc.queryForObject(
            "SELECT last_attempt_at FROM calendar_external_feeds WHERE id = ?", java.sql.Timestamp.class, UUID.fromString(id)).toInstant();
        String others = "/api/spaces/" + bobsSpaceId + "/calendar/external-feeds/" + id;

        mockMvc.perform(patch(others).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Piraté\",\"color\":\"event-indigo\"}"))
            .andExpect(status().isNotFound());
        mockMvc.perform(post(others + "/refresh").cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNotFound());
        mockMvc.perform(delete(others).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNotFound());

        // Unchanged: still there, under its name, and never asked for again.
        mockMvc.perform(get(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id))
            .andExpect(jsonPath("$[0].name").value("Vacances scolaires"));
        assertThat(jdbc.queryForObject("SELECT last_attempt_at FROM calendar_external_feeds WHERE id = ?",
            java.sql.Timestamp.class, UUID.fromString(id)).toInstant()).isEqualTo(attemptBefore);
        verify(downloader, org.mockito.Mockito.times(1)).download(any(), any(), any());
    }

    @Test
    void the_eleventh_calendar_of_a_space_is_refused() throws Exception {
        // Made directly: ten follows in a minute would meet the route's rate limit first.
        for (int i = 0; i < 10; i++) {
            externalFeeds.createUnlessFollowing(10, spaceId, "Calendrier " + i,
                java.net.URI.create("https://ecole.example/v" + i + ".ics"), null, aliceId, SCHOOL, null, null,
                Instant.parse("2026-10-13T12:00:00Z"));
        }
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ecole.example/v10.ics\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("too_many_feeds"));
    }

    @Test
    void an_address_that_is_no_address_is_refused_without_being_asked() throws Exception {
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"ftp://ecole.example/v.ics\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_feed_address"));
        verify(downloader, never()).download(any(), any(), any());
    }

    @Test
    void a_manual_refresh_keeps_what_the_address_now_answers() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, "\"v1\"", null));
        String id = objectMapper.readTree(mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andReturn().getResponse().getContentAsString()).get("id").asText();
        jdbc.update("UPDATE calendar_external_feeds SET last_attempt_at = now() - interval '2 minutes' WHERE space_id = ?", spaceId);
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.NotModified());

        mockMvc.perform(post(feeds() + "/" + id + "/refresh").cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastError").doesNotExist());
        // The attempt is spent: asked again at once, it is refused.
        mockMvc.perform(post(feeds() + "/" + id + "/refresh").cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void the_list_shows_only_the_calendars_of_its_own_space() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, null, null));
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ecole.example/v.ics\",\"name\":\"Chez Valentin\"}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/calendar/external-feeds").cookie(TestAccessTokens.cookieFor(bobId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://autre.example/v.ics\",\"name\":\"Chez Bob\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Chez Valentin"));
    }

    @Test
    void the_calendar_and_the_dashboard_show_a_followed_calendar() throws Exception {
        // Around today whatever the zone of the space: the dashboard reads today and tomorrow.
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        String around = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:x\r\n"
            + "BEGIN:VEVENT\r\nUID:pedago@ecole\r\nDTSTAMP:20260901T000000Z\r\nSUMMARY:Journée pédagogique\r\n"
            + "DTSTART;VALUE=DATE:" + from.format(DateTimeFormatter.BASIC_ISO_DATE) + "\r\n"
            + "DTEND;VALUE=DATE:" + from.plusDays(4).format(DateTimeFormatter.BASIC_ISO_DATE) + "\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(around, null, null));
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
            .content("{\"url\":\"https://ecole.example/v.ics\",\"color\":\"event-cyan\"}")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/spaces/" + spaceId + "/calendar/occurrences?from=" + from + "&to=" + from.plusDays(3))
                .cookie(TestAccessTokens.cookieFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.source == 'EXTERNAL')].title").value("Journée pédagogique"))
            .andExpect(jsonPath("$[?(@.source == 'EXTERNAL')].externalFeedId").isNotEmpty());
        mockMvc.perform(get("/api/spaces/" + spaceId + "/dashboard").cookie(TestAccessTokens.cookieFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards.agenda.data.allDay[?(@.title == 'Journée pédagogique')].color").value("event-cyan"));
    }

    @Test
    void a_refresh_bringing_new_content_shows_it_at_the_next_read() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, "\"v1\"", null));
        String id = objectMapper.readTree(mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andReturn().getResponse().getContentAsString()).get("id").asText();
        String october = "/api/spaces/" + spaceId + "/calendar/occurrences?from=2026-10-12&to=2026-10-25";
        mockMvc.perform(get(october).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(jsonPath("$[?(@.source == 'EXTERNAL')].title").value("Vacances de la Toussaint"));

        jdbc.update("UPDATE calendar_external_feeds SET last_attempt_at = now() - interval '2 minutes' WHERE space_id = ?", spaceId);
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(
            SCHOOL.replace("Vacances de la Toussaint", "Vacances d'automne"), "\"v2\"", null));
        mockMvc.perform(post(feeds() + "/" + id + "/refresh").cookie(TestAccessTokens.cookieFor(aliceId))).andExpect(status().isOk());

        mockMvc.perform(get(october).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(jsonPath("$[?(@.source == 'EXTERNAL')]", hasSize(1)))
            .andExpect(jsonPath("$[?(@.source == 'EXTERNAL')].title").value("Vacances d'automne"));
    }

    @Test
    void a_calendar_wears_a_colour_of_the_palette_and_the_graphite_of_external_sources_when_none_is_chosen() throws Exception {
        when(downloader.download(any(), any(), any())).thenReturn(new FeedDownload.Downloaded(SCHOOL, null, null));
        mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://ecole.example/v.ics\",\"color\":\"rouge\"}"))
            .andExpect(status().isBadRequest());
        String id = objectMapper.readTree(mockMvc.perform(post(feeds()).cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://ecole.example/v.ics\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.color").value("event-graphite"))
            .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch(feeds() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"École\",\"color\":\"#ff0000\"}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(patch(feeds() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"École\",\"color\":\"event-brique\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.color").value("event-brique"));
        mockMvc.perform(patch(feeds() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"École\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.color").value("event-graphite"));
    }

    private String feeds() {
        return "/api/spaces/" + spaceId + "/calendar/external-feeds";
    }

}
