package com.nido.api.dashboard.infrastructure.web;

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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class DashboardControllerIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcTemplate jdbc;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<UUID, String> emails = new HashMap<>();

    private LocalDate today;
    private UUID aliceId;
    private UUID bobId;
    private UUID carolId;
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
        emails.clear();
        today = LocalDate.now(PARIS);

        aliceId = saveUser("alice");
        bobId = saveUser("bob");
        carolId = saveUser("carol");
        spaceId = saveSpace(SpaceType.SHARED, "Chez Valentin", null);
        saveMembership(spaceId, aliceId, SpaceRole.OWNER);
        saveMembership(spaceId, bobId, SpaceRole.VIEWER);
        bobsSpaceId = saveSpace(SpaceType.SHARED, "Chez Bob", null);
        saveMembership(bobsSpaceId, bobId, SpaceRole.OWNER);
    }

    @Test
    void a_member_gets_every_block_of_a_busy_day() throws Exception {
        seedEvent("""
            {"title":"Anniversaire de Léa","allDay":true,"startDate":"%s","endDate":"%s"}""".formatted(today, today));
        seedEvent("""
            {"title":"Dîner chez Paul","allDay":false,"startDate":"%s","startTime":"19:30","endDate":"%s","endTime":"21:00"}"""
            .formatted(today, today));
        seedTask("""
            {"title":"Filtre de la hotte","priority":"HIGH","dueDate":"%s","assigneeIds":["%s"]}"""
            .formatted(today.minusDays(3), aliceId));
        seedTask("""
            {"title":"Payer la cantine","priority":"MED","dueDate":"%s"}""".formatted(today.plusDays(2)));
        seedTask("""
            {"title":"Arroser les plantes","priority":"LOW","dueDate":"%s"}""".formatted(today));
        String recipeId = createAndReadId(post(space() + "/kitchen/recipes"), """
            {"name":"Curry de lentilles","category":"VEGETARIAN","minutes":35,"referencePortions":4,
             "ingredients":[{"name":"Lentilles corail","quantity":200,"unit":"GRAM"}],"steps":["Cuire"]}""");
        createAndReadId(post(space() + "/kitchen/menu"), """
            {"date":"%s","recipeId":"%s","portions":4}""".formatted(today, recipeId));
        String aisleId = firstId(space() + "/shopping/categories");
        createAndReadId(post(space() + "/shopping/items"), """
            {"categoryId":"%s","name":"Tomates"}""".formatted(aisleId));
        JsonNode expenseCategory = firstExpenseCategory();
        String categoryId = expenseCategory.get("id").asText();
        mockMvc.perform(put(space() + "/finance/budgets/" + categoryId)
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"monthlyLimit\":10.00}"))
            .andExpect(status().is2xxSuccessful());
        createAndReadId(post(space() + "/finance/transactions"), """
            {"label":"Restaurant","amount":50.00,"type":"EXPENSE","categoryId":"%s","date":"%s"}"""
            .formatted(categoryId, today));
        createAndReadId(post(space() + "/finance/recurring-series"), """
            {"label":"Netflix","amount":13.49,"type":"EXPENSE","categoryId":"%s",
             "recurrence":{"intervalType":"WEEKLY","intervalCount":1,"anchorDate":"%s"}}"""
            .formatted(categoryId, today.plusDays(2)));
        createAndReadId(post(space() + "/finance/savings-goals"), """
            {"name":"Vacances","targetAmount":1200.00,"targetDate":"%s","color":"#5c7a58","glyph":"🎯"}"""
            .formatted(today.plusDays(100)));
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/invitations")
                .cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"alice@test.com\",\"role\":\"MEMBER\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(dashboard()).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.date").value(today.toString()))
            .andExpect(jsonPath("$.spaceType").value("SHARED"))
            .andExpect(jsonPath("$.canWrite").value(true))
            .andExpect(jsonPath("$.complete").value(true))
            .andExpect(jsonPath("$.attention.length()").value(3))
            .andExpect(jsonPath("$.attention[0].kind").value("OVERDUE_TASKS"))
            .andExpect(jsonPath("$.attention[0].severity").value("HIGH"))
            .andExpect(jsonPath("$.attention[0].count").value(1))
            .andExpect(jsonPath("$.attention[0].titles[0]").value("Filtre de la hotte"))
            .andExpect(jsonPath("$.attention[1].kind").value("BUDGET_OVERRUN"))
            .andExpect(jsonPath("$.attention[1].label").value(expenseCategory.get("label").asText()))
            .andExpect(jsonPath("$.attention[1].spent").value(50.0))
            .andExpect(jsonPath("$.attention[1].limit").value(10.0))
            .andExpect(jsonPath("$.attention[2].kind").value("INVITATION"))
            .andExpect(jsonPath("$.attention[2].spaceName").value("Chez Bob"))
            .andExpect(jsonPath("$.attention[2].invitedByUsername").value("bob"))
            .andExpect(jsonPath("$.cards.agenda.status").value("OK"))
            .andExpect(jsonPath("$.cards.agenda.data.allDay[0].title").value("Anniversaire de Léa"))
            .andExpect(jsonPath("$.cards.agenda.data.timed[0].title").value("Dîner chez Paul"))
            .andExpect(jsonPath("$.cards.agenda.data.timed[0].startTime").value("19:30:00"))
            .andExpect(jsonPath("$.cards.agenda.data.dueToday[0].title").value("Arroser les plantes"))
            .andExpect(jsonPath("$.cards.agenda.data.tomorrow").value(nullValue()))
            .andExpect(jsonPath("$.cards.menu.data.today[0].recipeName").value("Curry de lentilles"))
            .andExpect(jsonPath("$.cards.menu.data.today[0].category").value("VEGETARIAN"))
            .andExpect(jsonPath("$.cards.menu.data.today[0].minutes").value(35))
            .andExpect(jsonPath("$.cards.menu.data.today[0].portions").value(4))
            .andExpect(jsonPath("$.cards.menu.data.unplannedDays.length()").value(6))
            .andExpect(jsonPath("$.cards.tasks.data.overdue[0].title").value("Filtre de la hotte"))
            .andExpect(jsonPath("$.cards.tasks.data.thisWeek[0].title").value("Payer la cantine"))
            .andExpect(jsonPath("$.cards.tasks.data.openCount").value(3))
            .andExpect(jsonPath("$.cards.finance.data.month").value(YearMonth.from(today).toString()))
            .andExpect(jsonPath("$.cards.finance.data.budgetsToWatch[0].status").value("OVER"))
            .andExpect(jsonPath("$.cards.finance.data.upcoming[0].label").value("Netflix"))
            .andExpect(jsonPath("$.cards.finance.data.balances.length()").value(0))
            .andExpect(jsonPath("$.cards.savings.data.goals[0].name").value("Vacances"))
            .andExpect(jsonPath("$.cards.savings.data.goals[0].state").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.cards.shopping.data.remaining").value(1))
            .andExpect(jsonPath("$.cards.shopping.data.categories[0].preview[0]").value("Tomates"));
    }

    @Test
    void a_viewer_reads_the_dashboard_without_write_rights() throws Exception {
        mockMvc.perform(get(dashboard()).cookie(tokenFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.canWrite").value(false));
    }

    @Test
    void an_outsider_gets_404() throws Exception {
        mockMvc.perform(get(dashboard()).cookie(tokenFor(carolId)))
            .andExpect(status().isNotFound());
    }

    @Test
    void a_personal_space_has_neither_balances_nor_savings() throws Exception {
        UUID personalId = saveSpace(SpaceType.PERSONAL, "Perso", aliceId);
        saveMembership(personalId, aliceId, SpaceRole.OWNER);

        mockMvc.perform(get("/api/spaces/" + personalId + "/dashboard").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.spaceType").value("PERSONAL"))
            .andExpect(jsonPath("$.cards.finance.status").value("OK"))
            .andExpect(jsonPath("$.cards.finance.data.balances").value(nullValue()))
            .andExpect(jsonPath("$.cards.savings").doesNotExist());
    }

    @Test
    void an_empty_shared_space_shows_only_the_agenda_the_menu_and_the_finances() throws Exception {
        mockMvc.perform(get(dashboard()).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attention.length()").value(0))
            .andExpect(jsonPath("$.cards.agenda.status").value("OK"))
            .andExpect(jsonPath("$.cards.menu.status").value("OK"))
            .andExpect(jsonPath("$.cards.finance.status").value("OK"))
            .andExpect(jsonPath("$.cards.tasks").doesNotExist())
            .andExpect(jsonPath("$.cards.savings").doesNotExist())
            .andExpect(jsonPath("$.cards.shopping").doesNotExist());
    }

    // Commit 83fa623: the calendar, opened before the tasks page, created the due recurring tasks inside
    // its read-only transaction and answered 500. The dashboard is the page the app now opens on, so it
    // is the first reader of a due occurrence far more often than not: it must create it, in its own
    // transaction, and show it with the member whose turn it is.
    @Test
    void the_dashboard_opened_first_creates_the_due_recurring_task_and_shows_it_with_its_turn() throws Exception {
        mockMvc.perform(post(space() + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Poubelles","priority":"MED","recurrence":{"intervalType":"WEEKLY","intervalCount":1,
                     "leadIntervalType":"DAILY","leadIntervalCount":0,"anchorDate":"%s",
                     "rotationMemberIds":["%s","%s"]}}""".formatted(today.minusDays(7), bobId, aliceId)))
            .andExpect(status().isCreated());
        Integer before = jdbc.queryForObject("SELECT count(*) FROM tasks WHERE space_id = ?", Integer.class, spaceId);

        mockMvc.perform(get(dashboard()).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cards.agenda.data.dueToday[0].title").value("Poubelles"))
            .andExpect(jsonPath("$.cards.agenda.data.dueToday[0].assigneeIds[0]").value(aliceId.toString()))
            .andExpect(jsonPath("$.cards.agenda.data.dueToday[0].recurring").value(true))
            // Last week's occurrence was Bob's turn: overdue, but not Alice's to act on.
            .andExpect(jsonPath("$.cards.tasks.data.overdue[0].assigneeIds[0]").value(bobId.toString()))
            .andExpect(jsonPath("$.attention[?(@.kind == 'OVERDUE_TASKS')]").isEmpty());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE space_id = ?", Integer.class, spaceId))
            .isEqualTo(before + 1);
    }

    private void seedEvent(String body) throws Exception {
        mockMvc.perform(post(space() + "/calendar/events")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
    }

    private void seedTask(String body) throws Exception {
        mockMvc.perform(post(space() + "/tasks")
                .cookie(tokenFor(aliceId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
    }

    private String createAndReadId(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body) throws Exception {
        String response = mockMvc.perform(request.cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String firstId(String url) throws Exception {
        String body = mockMvc.perform(get(url).cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get(0).get("id").asText();
    }

    private JsonNode firstExpenseCategory() throws Exception {
        String body = mockMvc.perform(get(space() + "/finance/categories").cookie(tokenFor(aliceId)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode category : objectMapper.readTree(body)) {
            if (category.get("type").asText().equals("EXPENSE")) {
                return category;
            }
        }
        throw new IllegalStateException("no expense category seeded");
    }

    private String space() {
        return "/api/spaces/" + spaceId;
    }

    private String dashboard() {
        return space() + "/dashboard";
    }

    private UUID saveUser(String username) {
        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername(username);
        user.setEmail(username + "@test.com");
        user.setRole(Role.USER);
        UUID id = users.saveAndFlush(user).getId();
        emails.put(id, username + "@test.com");
        return id;
    }

    private UUID saveSpace(SpaceType type, String name, UUID personalOwnerId) {
        SpaceEntity space = new SpaceEntity();
        space.setType(type);
        space.setName(name);
        space.setAccent(type == SpaceType.PERSONAL ? "#8a7d6b" : "#c17a5c");
        space.setGlyph(type == SpaceType.PERSONAL ? "👤" : "🏡");
        if (personalOwnerId != null) {
            space.setPersonalOwnerId(personalOwnerId);
            space.setCreatedBy(personalOwnerId);
        }
        return spaces.saveAndFlush(space).getId();
    }

    private void saveMembership(UUID space, UUID userId, SpaceRole role) {
        SpaceMemberEntity member = new SpaceMemberEntity();
        member.setSpaceId(space);
        member.setUserId(userId);
        member.setRole(role);
        members.saveAndFlush(member);
    }

    /** The e-mail claim is the user's real address: received invitations are looked up by it. */
    private Cookie tokenFor(UUID userId) {
        String token = Jwts.builder()
            .issuer("nido")
            .audience().add("nido").and()
            .subject(userId.toString())
            .claim("role", Role.USER.name())
            .claim("email", emails.get(userId))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
        return new Cookie("access_token", token);
    }
}
