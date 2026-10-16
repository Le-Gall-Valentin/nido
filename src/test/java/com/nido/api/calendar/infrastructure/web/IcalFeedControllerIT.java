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
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        settingsStore.save(Map.of(SettingKey.PUBLIC_URL, Optional.of("https://nido.test")), null, Instant.now());
    }

    @AfterEach
    void noPublicAddress() {
        InstanceSettingsTestSupport.clear(settingsStore);
        jdbc.update("DELETE FROM calendar_subscription_links");
    }

    @Test
    void a_calendar_application_reads_the_address_without_signing_in() throws Exception {
        createEvent("Concert", "2026-09-23");
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        mockMvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/calendar;charset=UTF-8"))
            .andExpect(header().string("X-Robots-Tag", "noindex"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SUMMARY:Concert")));
        assertThat(jdbc.queryForObject("SELECT last_read_at IS NOT NULL FROM calendar_subscription_links", Boolean.class)).isTrue();
    }

    @Test
    void unknown_revoked_and_orphaned_addresses_all_answer_the_same_404() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String unknown = "/ical/" + SubscriptionTokens.newToken() + ".ics";
        mockMvc.perform(get(unknown)).andExpect(status().isNotFound());
        mockMvc.perform(get("/ical/not-a-token.ics")).andExpect(status().isNotFound());

        // Bob leaves the space behind the application's back: his address reads nothing any more.
        jdbc.update("DELETE FROM space_members WHERE user_id = ?", bobId);
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
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
