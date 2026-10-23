package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestAccessTokens;
import com.nido.api.TestPeople;
import com.nido.api.calendar.domain.model.CalendarDigests;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
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

        aliceId = TestPeople.user(users, "alice");
        bobId = TestPeople.user(users, "bob");
        spaceId = TestPeople.sharedSpace(spaces, "Chez Valentin");
        TestPeople.member(members, spaceId, aliceId, SpaceRole.OWNER);
        TestPeople.member(members, spaceId, bobId, SpaceRole.VIEWER);
        bobsSpaceId = TestPeople.sharedSpace(spaces, "Chez Bob");
        TestPeople.member(members, bobsSpaceId, bobId, SpaceRole.OWNER);
        settingsStore.save(Map.of(SettingKey.PUBLIC_URL, Optional.of("https://nido.test")), null, Instant.now());
    }

    @AfterEach
    void noPublicAddress() {
        InstanceSettingsTestSupport.clear(settingsStore);
        // The database is shared with every other test: only this one's rows are touched.
        jdbc.update("DELETE FROM calendar_subscription_links WHERE user_id IN (?, ?)", aliceId, bobId);
    }

    @Test
    void a_viewer_creates_an_address_shown_once_with_its_webcal_twin() throws Exception {
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"iPhone\",\"sources\":[\"EVENT\",\"TASK\"],\"onlyMine\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.matchesPattern("https://nido\\.test/ical/[A-Za-z0-9_-]{43}\\.ics")))
            .andExpect(jsonPath("$.webcalUrl").value(org.hamcrest.Matchers.matchesPattern("webcal://nido\\.test/ical/[A-Za-z0-9_-]{43}\\.ics")))
            .andExpect(jsonPath("$.link.label").value("iPhone"))
            .andExpect(jsonPath("$.link.sources[0]").value("EVENT"))
            .andExpect(jsonPath("$.link.onlyMine").value(true));

        mockMvc.perform(get(links()).cookie(TestAccessTokens.cookieFor(bobId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].label").value("iPhone"))
            .andExpect(jsonPath("$[0].url").doesNotExist())
            .andExpect(jsonPath("$[0].token").doesNotExist())
            .andExpect(jsonPath("$[0].tokenHash").doesNotExist())
            .andExpect(jsonPath("$[0].lastReadAt").doesNotExist());

        mockMvc.perform(get(links()).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void the_label_is_sealed_and_only_the_tokens_digest_is_kept() throws Exception {
        String url = create(bobId, "T\u00e9l\u00e9phone de Bob");
        String token = url.substring(url.lastIndexOf('/') + 1, url.length() - ".ics".length());

        Map<String, Object> row = jdbc.queryForMap("SELECT label_encrypted, token_hash FROM calendar_subscription_links WHERE user_id = ?",
            bobId);
        assertThat((String) row.get("label_encrypted")).startsWith("v3:").doesNotContain("Bob");
        assertThat(row.get("token_hash")).isEqualTo(CalendarDigests.sha256Hex(token));
    }

    @Test
    void an_address_is_revoked_by_its_owner_only() throws Exception {
        create(bobId, "iPhone");
        String id = jdbc.queryForObject("SELECT id::text FROM calendar_subscription_links WHERE user_id = ?", String.class, bobId);

        mockMvc.perform(delete(links() + "/" + id).cookie(TestAccessTokens.cookieFor(aliceId))).andExpect(status().isNotFound());
        mockMvc.perform(delete(links() + "/" + id).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNoContent());
        mockMvc.perform(get(links()).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void an_address_is_revoked_without_its_label_being_opened() throws Exception {
        create(bobId, "iPhone");
        create(bobId, "Tablette");
        List<String> ids = jdbc.queryForList("SELECT id::text FROM calendar_subscription_links WHERE space_id = ? ORDER BY created_at",
            String.class, spaceId);
        // The first address now holds the label sealed for the second: opening it would be refused.
        jdbc.update("UPDATE calendar_subscription_links SET label_encrypted = (SELECT label_encrypted FROM calendar_subscription_links "
            + "WHERE id = ?::uuid) WHERE id = ?::uuid", ids.get(1), ids.get(0));

        mockMvc.perform(delete(links() + "/" + ids.get(0)).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_subscription_links WHERE id = ?::uuid", Long.class, ids.get(0)))
            .isZero();
    }

    @Test
    void ten_addresses_per_person_and_space_at_most() throws Exception {
        for (int i = 0; i < 10; i++) {
            create(bobId, "Appareil " + i);
        }
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"Un de trop\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("too_many_links"));

        // The cap counts per person and per space: neither Alice here nor Bob elsewhere is held back.
        create(aliceId, "Premier");
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/calendar/subscription-links").cookie(TestAccessTokens.cookieFor(bobId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Ailleurs\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isCreated());
    }

    @Test
    void an_address_of_another_space_is_not_revoked_through_this_one() throws Exception {
        mockMvc.perform(post("/api/spaces/" + bobsSpaceId + "/calendar/subscription-links").cookie(TestAccessTokens.cookieFor(bobId))
                .contentType(MediaType.APPLICATION_JSON).content("{\"label\":\"Chez Bob\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isCreated());
        String id = jdbc.queryForObject("SELECT id::text FROM calendar_subscription_links WHERE user_id = ?", String.class, bobId);

        mockMvc.perform(delete(links() + "/" + id).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_subscription_links WHERE user_id = ?", Long.class, bobId))
            .isEqualTo(1L);
    }

    @Test
    void a_null_source_is_refused() throws Exception {
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[null]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void a_source_nido_does_not_know_is_an_invalid_source_as_on_export() throws Exception {
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[\"BOGUS\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
    }

    @Test
    void a_followed_calendar_cannot_be_subscribed_to_again() throws Exception {
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[\"EXTERNAL\"]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error_code").value("invalid_sources"));
    }

    @Test
    void without_a_public_address_no_address_can_be_given() throws Exception {
        InstanceSettingsTestSupport.clear(settingsStore);
        mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(bobId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"x\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error_code").value("public_url_missing"));
    }

    private String create(UUID userId, String label) throws Exception {
        String body = mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(userId)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"" + label + "\",\"sources\":[\"EVENT\"]}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).get("url").asText();
    }

    private String links() {
        return "/api/spaces/" + spaceId + "/calendar/subscription-links";
    }

}
