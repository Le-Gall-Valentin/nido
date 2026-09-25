package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarOccurrencesControllerIT {

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
    void one_request_returns_every_source_of_the_window_in_date_order() throws Exception {
        seedEvent();
        seedTask();
        seedRecurringFinanceSeries();
        seedSavingsGoal();

        mockMvc.perform(get(occurrences() + "?from=2026-03-01&to=2026-03-31").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].source").value("TASK"))
            .andExpect(jsonPath("$[0].startDate").value("2026-03-05"))
            .andExpect(jsonPath("$[1].source").value("EVENT"))
            .andExpect(jsonPath("$[1].startDate").value("2026-03-10"))
            .andExpect(jsonPath("$[2].source").value("FINANCE"))
            .andExpect(jsonPath("$[2].startDate").value("2026-03-15"))
            .andExpect(jsonPath("$[3].source").value("SAVINGS"))
            .andExpect(jsonPath("$[3].startDate").value("2026-03-20"));
    }

    @Test
    void a_source_outside_the_window_does_not_appear() throws Exception {
        seedEvent();
        seedTask();

        mockMvc.perform(get(occurrences() + "?from=2026-03-08&to=2026-03-12").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].source").value("EVENT"));
    }

    @Test
    void a_reversed_window_is_rejected_with_400() throws Exception {
        // Named for what it is: it used to say "this window covers 0 days", as if it were too long.
        mockMvc.perform(get(occurrences() + "?from=2026-03-31&to=2026-03-01").cookie(tokenFor(aliceId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("ReversedWindow"));
    }

    @Test
    void a_task_without_a_due_date_never_reaches_the_calendar() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Sans échéance\",\"priority\":\"MED\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(occurrences() + "?from=2026-01-01&to=2026-12-31").cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    // A recurring operation or task is shown from its real rows for what has happened, and from the
    // projection for what is still to come. Each date must come out exactly once — the past, today
    // and the future — once the finance or tasks page has turned what was due into real rows.

    @Test
    void a_recurring_operation_shows_once_on_its_past_days_today_and_after() throws Exception {
        LocalDate start = LocalDate.now().minusWeeks(2);
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/recurring-series")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"label":"Abonnement","amount":9.99,"type":"EXPENSE","categoryId":"%s",
                     "recurrence":{"intervalType":"WEEKLY","intervalCount":1,"anchorDate":"%s"}}"""
                    .formatted(firstCategoryId(), start)))
            .andExpect(status().isCreated());
        // Opening the finance page materializes every occurrence due so far, today's included.
        mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/transactions?month="
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk());

        List<LocalDate> shown = datesOf("FINANCE", start, start.plusWeeks(5));

        assertThat(shown).doesNotHaveDuplicates().hasSize(6);
    }

    @Test
    void a_recurring_task_shows_once_on_its_past_days_today_and_after() throws Exception {
        LocalDate start = LocalDate.now().minusWeeks(2);
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Poubelles","priority":"MED","recurrence":{"intervalType":"WEEKLY","intervalCount":1,
                     "leadIntervalType":"DAILY","leadIntervalCount":0,"anchorDate":"%s"}}""".formatted(start)))
            .andExpect(status().isCreated());
        // Opening the tasks page materializes every occurrence due so far.
        mockMvc.perform(get("/api/spaces/" + spaceId + "/tasks").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk());

        List<LocalDate> shown = datesOf("TASK", start, start.plusWeeks(5));

        assertThat(shown).doesNotHaveDuplicates().hasSize(6);
    }

    // The calendar may well be the first page opened once a recurring task has fallen due. Its read
    // used to create the due tasks inside a read-only transaction, which failed the whole calendar with
    // a 500 until someone opened the tasks page. A read shows what is due and writes nothing.
    @Test
    void the_calendar_opened_first_shows_a_due_recurring_task_once_and_writes_nothing() throws Exception {
        LocalDate start = LocalDate.now().minusWeeks(2);
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Poubelles","priority":"MED","recurrence":{"intervalType":"WEEKLY","intervalCount":1,
                     "leadIntervalType":"DAILY","leadIntervalCount":0,"anchorDate":"%s"}}""".formatted(start)))
            .andExpect(status().isCreated());
        Integer tasksBefore = jdbc.queryForObject("SELECT count(*) FROM tasks WHERE space_id = ?", Integer.class, spaceId);

        List<LocalDate> shown = datesOf("TASK", start, start.plusWeeks(5));

        assertThat(shown).doesNotHaveDuplicates().hasSize(6);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE space_id = ?", Integer.class, spaceId))
            .isEqualTo(tasksBefore);
    }

    private List<LocalDate> datesOf(String source, LocalDate from, LocalDate to) throws Exception {
        String body = mockMvc.perform(get(occurrences() + "?from=" + from + "&to=" + to).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<LocalDate> dates = new ArrayList<>();
        for (JsonNode occurrence : objectMapper.readTree(body)) {
            if (occurrence.get("source").asText().equals(source)) {
                dates.add(LocalDate.parse(occurrence.get("startDate").asText()));
            }
        }
        return dates;
    }

    private String firstCategoryId() throws Exception {
        String categories = mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/categories")
                .cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(categories).get(0).get("id").asText();
    }

    private void seedEvent() throws Exception {
        mockMvc.perform(post(events())
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"RDV dentiste","allDay":true,"startDate":"2026-03-10","endDate":"2026-03-10"}"""))
            .andExpect(status().isCreated());
    }

    private void seedTask() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Poubelles\",\"priority\":\"MED\",\"dueDate\":\"2026-03-05\"}"))
            .andExpect(status().isCreated());
    }

    private void seedRecurringFinanceSeries() throws Exception {
        String categories = mockMvc.perform(get("/api/spaces/" + spaceId + "/finance/categories")
                .cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String categoryId = objectMapper.readTree(categories).get(0).get("id").asText();

        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/recurring-series")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"label":"Loyer","amount":850.00,"type":"EXPENSE","categoryId":"%s",
                     "recurrence":{"intervalType":"MONTHLY","intervalCount":1,"anchorDate":"2026-03-15"}}"""
                    .formatted(categoryId)))
            .andExpect(status().isCreated());
    }

    private void seedSavingsGoal() throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/finance/savings-goals")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Vacances","targetAmount":1200.00,"targetDate":"2026-03-20",
                     "color":"#5c7a58","glyph":"🎯"}"""))
            .andExpect(status().isCreated());
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
