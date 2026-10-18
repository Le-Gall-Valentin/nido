package com.nido.api.calendar.infrastructure.web;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarExportControllerIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;

    private MockMvc mockMvc;

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
    void a_viewer_downloads_the_spaces_events_as_an_ics_file() throws Exception {
        createEvent("Concert", "2026-09-23");
        createSeries();

        MvcResult result = mockMvc.perform(get(export() + "?sources=EVENT").cookie(tokenFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"nido-chez-valentin-")))
            .andReturn();
        String ics = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(ics).contains("X-WR-CALNAME:Chez Valentin", "SUMMARY:Concert", "SUMMARY:Piano", "RRULE:");
    }

    @Test
    void only_what_concerns_me_leaves_out_the_events_of_others() throws Exception {
        createEventWith("Rendez-vous de Bob", "2026-09-24", bobId);
        createEvent("Concert", "2026-09-23");

        String ics = mockMvc.perform(get(export() + "?sources=EVENT&onlyMine=true").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(ics).contains("SUMMARY:Concert").doesNotContain("Rendez-vous de Bob");
    }

    @Test
    void a_followed_calendar_an_unknown_source_or_no_source_at_all_is_a_400() throws Exception {
        mockMvc.perform(get(export() + "?sources=EXTERNAL").cookie(tokenFor(aliceId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
        mockMvc.perform(get(export() + "?sources=EVENT,BOGUS").cookie(tokenFor(aliceId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
        mockMvc.perform(get(export()).cookie(tokenFor(aliceId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
    }

    @Test
    void only_the_sources_asked_for_are_written() throws Exception {
        createEvent("Concert", "2026-09-23");
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks").cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Arroser les plantes\",\"priority\":\"MED\",\"dueDate\":\""
                    + LocalDate.now(ZoneId.of("Europe/Paris")).plusDays(7) + "\"}"))
            .andExpect(status().isCreated());

        String events = mockMvc.perform(get(export() + "?sources=EVENT").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String both = mockMvc.perform(get(export() + "?sources=EVENT,TASK").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String tasks = mockMvc.perform(get(export() + "?sources=TASK").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(events).contains("SUMMARY:Concert").doesNotContain("Arroser les plantes");
        assertThat(both).contains("SUMMARY:Concert", "Arroser les plantes", "UID:TASK-");
        assertThat(tasks).contains("Arroser les plantes").doesNotContain("SUMMARY:Concert");
    }

    @Test
    void the_file_name_is_also_given_in_utf_8() throws Exception {
        mockMvc.perform(get(export() + "?sources=EVENT").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition",
                org.hamcrest.Matchers.containsString("filename*=UTF-8''nido-chez-valentin-")));
    }

    @Test
    void someone_outside_the_space_gets_a_404() throws Exception {
        UUID carolId = saveUser("carol");
        mockMvc.perform(get(export() + "?sources=EVENT").cookie(tokenFor(carolId))).andExpect(status().isNotFound());
    }

    private void createEvent(String title, String day) throws Exception {
        createEventWith(title, day, null);
    }

    private void createEventWith(String title, String day, UUID participant) throws Exception {
        String participants = participant == null ? "[]" : "[\"" + participant + "\"]";
        mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/events").cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"allDay\":true,\"startDate\":\"" + day + "\",\"endDate\":\"" + day
                    + "\",\"participantIds\":" + participants + "}"))
            .andExpect(status().isCreated());
    }

    private void createSeries() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/recurring-event-series").cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Piano","allDay":false,"startTime":"18:00","endTime":"19:00","intervalType":"WEEKLY",
                     "intervalCount":1,"weekdays":["MONDAY","THURSDAY"],"anchorDate":"2026-01-05"}"""))
            .andExpect(status().isCreated());
    }

    private String export() {
        return "/api/spaces/" + spaceId + "/calendar/export.ics";
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
