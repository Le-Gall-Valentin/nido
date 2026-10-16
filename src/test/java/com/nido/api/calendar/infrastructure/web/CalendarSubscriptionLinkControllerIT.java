package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.calendar.domain.model.CalendarDigests;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTestConfig
class CalendarSubscriptionLinkControllerIT {

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
    }

    @BeforeEach
    void publicAddress() {
        settingsStore.save(Map.of(SettingKey.PUBLIC_URL, Optional.of("https://nido.test")), null, Instant.now());
    }

    @AfterEach
    void noPublicAddress() {
        InstanceSettingsTestSupport.clear(settingsStore);
        jdbc.update("DELETE FROM calendar_subscription_links");
    }

    @Test
    void a_viewer_creates_an_address_shown_once_with_its_webcal_twin() throws Exception {
        mockMvc.perform(post(links()).cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"iPhone\",\"sources\":[\"EVENT\",\"TASK\"],\"onlyMine\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.matchesPattern("https://nido\\.test/ical/[A-Za-z0-9_-]{43}\\.ics")))
            .andExpect(jsonPath("$.webcalUrl").value(org.hamcrest.Matchers.matchesPattern("webcal://nido\\.test/ical/[A-Za-z0-9_-]{43}\\.ics")))
            .andExpect(jsonPath("$.link.label").value("iPhone"))
            .andExpect(jsonPath("$.link.sources[0]").value("EVENT"))
            .andExpect(jsonPath("$.link.onlyMine").value(true));

        mockMvc.perform(get(links()).cookie(tokenFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].label").value("iPhone"))
            .andExpect(jsonPath("$[0].url").doesNotExist())
            .andExpect(jsonPath("$[0].lastReadAt").doesNotExist());

        mockMvc.perform(get(links()).cookie(tokenFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void the_label_is_sealed_and_only_the_tokens_digest_is_kept() throws Exception {
        String url = create(bobId, "T\u00e9l\u00e9phone de Bob");
        String token = url.substring(url.lastIndexOf('/') + 1, url.length() - ".ics".length());

        Map<String, Object> row = jdbc.queryForMap("SELECT label_encrypted, token_hash FROM calendar_subscription_links");
        assertThat((String) row.get("label_encrypted")).startsWith("v3:").doesNotContain("Bob");
        assertThat(row.get("token_hash")).isEqualTo(CalendarDigests.sha256Hex(token));
    }

    @Test
    void an_address_is_revoked_by_its_owner_only() throws Exception {
        create(bobId, "iPhone");
        String id = jdbc.queryForObject("SELECT id::text FROM calendar_subscription_links", String.class);

        mockMvc.perform(delete(links() + "/" + id).cookie(tokenFor(aliceId))).andExpect(status().isNotFound());
        mockMvc.perform(delete(links() + "/" + id).cookie(tokenFor(bobId))).andExpect(status().isNoContent());
        mockMvc.perform(get(links()).cookie(tokenFor(bobId))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ten_addresses_per_person_and_space_at_most() throws Exception {
        for (int i = 0; i < 10; i++) {
            create(bobId, "Appareil " + i);
        }
        mockMvc.perform(post(links()).cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Un de trop\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("too_many_links"));
    }

    @Test
    void a_followed_calendar_cannot_be_subscribed_to_again() throws Exception {
        mockMvc.perform(post(links()).cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[\"EXTERNAL\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
    }

    @Test
    void without_a_public_address_no_address_can_be_given() throws Exception {
        InstanceSettingsTestSupport.clear(settingsStore);
        mockMvc.perform(post(links()).cookie(tokenFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("public_url_missing"));
    }

    private String create(UUID userId, String label) throws Exception {
        String body = mockMvc.perform(post(links()).cookie(tokenFor(userId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"" + label + "\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("url").asText();
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
