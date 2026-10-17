package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.calendar.domain.model.SubscriptionTokens;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class IcalFeedControllerIT {

    private static final String JWT_SECRET = "integration-test-secret-at-least-32-chars!";

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcTemplate jdbc;
    @Autowired SettingsStorePort settingsStore;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        settingsStore.save(Map.of(SettingKey.PUBLIC_URL, Optional.of("https://nido.test")), null, Instant.now());
    }

    @AfterEach
    void noPublicAddress() {
        InstanceSettingsTestSupport.clear(settingsStore);
        jdbc.update("DELETE FROM calendar_subscription_links");
        jdbc.update("UPDATE users SET is_active = true");
    }

    @Test
    void a_calendar_application_reads_the_address_without_signing_in() throws Exception {
        createEvent("Concert", "2026-09-23");
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
            .andExpect(header().string("Cache-Control", "max-age=0, private"))
            .andExpect(header().string("X-Robots-Tag", "noindex"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SUMMARY:Concert")));
        assertThat(jdbc.queryForObject("SELECT last_read_at IS NOT NULL FROM calendar_subscription_links", Boolean.class)).isTrue();
    }

    @Test
    void a_head_request_is_answered_like_the_get() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        mockMvc.perform(head(path))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"));
    }

    @Test
    void an_invalid_session_cookie_changes_nothing() throws Exception {
        createEvent("Concert", "2026-09-23");
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String plain = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String withCookie = mockMvc.perform(get(path).cookie(new Cookie("access_token", "not.a.jwt")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Each rendering carries its own time stamp: everything else is the same.
        assertThat(withCookie.replaceAll("DTSTAMP:\\S+", "")).contains("SUMMARY:Concert")
            .isEqualTo(plain.replaceAll("DTSTAMP:\\S+", ""));
    }

    @Test
    void unknown_revoked_orphaned_and_deactivated_addresses_all_answer_the_same_404() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String alicesPath = createLink(aliceId, "{\"label\":\"Mac\",\"sources\":[\"EVENT\"]}");

        List<MockHttpServletResponse> answers = new ArrayList<>();
        answers.add(mockMvc.perform(get("/ical/not-a-token.ics")).andReturn().getResponse());
        answers.add(mockMvc.perform(get("/ical/" + SubscriptionTokens.newToken() + ".ics")).andReturn().getResponse());

        // Bob leaves the space behind the application's back: his address reads nothing any more.
        jdbc.update("DELETE FROM space_members WHERE user_id = ?", bobId);
        answers.add(mockMvc.perform(get(path)).andReturn().getResponse());

        // Alice is deactivated: her address reads nothing either, until she is back.
        jdbc.update("UPDATE users SET is_active = false WHERE id = ?", aliceId);
        answers.add(mockMvc.perform(get(alicesPath)).andReturn().getResponse());
        jdbc.update("UPDATE users SET is_active = true WHERE id = ?", aliceId);
        mockMvc.perform(get(alicesPath)).andExpect(status().isOk());

        MockHttpServletResponse first = answers.get(0);
        assertThat(first.getStatus()).isEqualTo(404);
        for (MockHttpServletResponse answer : answers) {
            assertThat(answer.getStatus()).isEqualTo(first.getStatus());
            assertThat(answer.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(first.getContentAsString(StandardCharsets.UTF_8));
            assertThat(answer.getContentType()).isEqualTo(first.getContentType());
        }
    }

    @Test
    void a_revoked_address_reads_nothing() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String id = jdbc.queryForObject("SELECT id::text FROM calendar_subscription_links", String.class);
        mockMvc.perform(delete(links() + "/" + id).cookie(tokenFor(bobId))).andExpect(status().isNoContent());

        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

    /** The path of the address made, as a calendar application would request it. */
    private String createLink(UUID userId, String body) throws Exception {
        String response = mockMvc.perform(post(links()).cookie(tokenFor(userId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return URI.create(objectMapper.readTree(response).get("url").asText()).getPath();
    }

    private void createEvent(String title, String day) throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/events").cookie(tokenFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"allDay\":true,\"startDate\":\"" + day + "\",\"endDate\":\"" + day
                    + "\",\"participantIds\":[]}"))
            .andExpect(status().isCreated());
    }

    private String links() {
        return "/api/spaces/" + spaceId + "/calendar/subscription-links";
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
