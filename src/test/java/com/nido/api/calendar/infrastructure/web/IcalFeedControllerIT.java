package com.nido.api.calendar.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestAccessTokens;
import com.nido.api.TestPeople;
import com.nido.api.calendar.infrastructure.security.NidoSubscriptionTokenGenerator;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.infrastructure.ratelimit.RedisRateLimitBucketStore;
import com.nido.api.instance.InstanceSettingsTestSupport;
import com.nido.api.instance.domain.model.SettingKey;
import com.nido.api.instance.domain.port.out.SettingsStorePort;
import com.nido.api.space.application.port.in.HandleUserDeletionUseCase;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import com.nido.api.space.infrastructure.persistence.repository.SpaceMemberJpaRepository;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
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

    @Autowired WebApplicationContext webApplicationContext;
    @Autowired UserIdentityJpaRepository users;
    @Autowired SpaceJpaRepository spaces;
    @Autowired SpaceMemberJpaRepository members;
    @Autowired RedisRateLimitBucketStore rateLimitBucketStore;
    @Autowired JdbcTemplate jdbc;
    @Autowired SettingsStorePort settingsStore;
    @Autowired HandleUserDeletionUseCase userDeletion;

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

        aliceId = TestPeople.user(users, "alice");
        bobId = TestPeople.user(users, "bob");
        spaceId = TestPeople.sharedSpace(spaces, "Chez Valentin");
        TestPeople.member(members, spaceId, aliceId, SpaceRole.OWNER);
        TestPeople.member(members, spaceId, bobId, SpaceRole.VIEWER);
        settingsStore.save(Map.of(SettingKey.PUBLIC_URL, Optional.of("https://nido.test")), null, Instant.now());
    }

    @AfterEach
    void noPublicAddress() {
        InstanceSettingsTestSupport.clear(settingsStore);
        // The database is shared with every other test: only this one's rows are touched.
        jdbc.update("DELETE FROM calendar_subscription_links WHERE user_id IN (?, ?)", aliceId, bobId);
        jdbc.update("UPDATE users SET is_active = true WHERE id IN (?, ?)", aliceId, bobId);
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
        assertThat(jdbc.queryForObject("SELECT last_read_at IS NOT NULL FROM calendar_subscription_links WHERE user_id = ?",
            Boolean.class, bobId)).isTrue();
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
        answers.add(mockMvc.perform(get("/ical/" + new NidoSubscriptionTokenGenerator().generate() + ".ics")).andReturn().getResponse());

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
        String id = jdbc.queryForObject("SELECT id::text FROM calendar_subscription_links WHERE user_id = ?", String.class, bobId);
        mockMvc.perform(delete(links() + "/" + id).cookie(TestAccessTokens.cookieFor(bobId))).andExpect(status().isNoContent());

        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test
    void a_member_removed_from_the_space_loses_their_addresses() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/members/" + bobId).cookie(TestAccessTokens.cookieFor(aliceId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_subscription_links WHERE space_id = ? AND user_id = ?",
            Long.class, spaceId, bobId)).isZero();
    }

    @Test
    void a_member_who_leaves_the_space_loses_their_addresses() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        mockMvc.perform(delete("/api/spaces/" + spaceId + "/membership").cookie(TestAccessTokens.cookieFor(bobId)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(path)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_subscription_links WHERE space_id = ? AND user_id = ?",
            Long.class, spaceId, bobId)).isZero();
    }

    @Test
    void a_deleted_account_loses_its_addresses_in_every_space_it_left() throws Exception {
        createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");

        userDeletion.handleUserDeletion(bobId, "bob");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_subscription_links WHERE user_id = ?", Long.class, bobId))
            .isZero();
    }

    /** The path of the address made, as a calendar application would request it. */
    @Test
    void no_answer_to_a_subscription_address_names_its_token() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String token = path.substring("/ical/".length(), path.length() - ".ics".length());

        // Another method falls to the security chain; another case of the suffix, to Spring's own static resources.
        MockHttpServletResponse posted = mockMvc.perform(post(path)).andReturn().getResponse();
        MockHttpServletResponse shouted = mockMvc.perform(get(path.replace(".ics", ".ICS"))).andReturn().getResponse();

        assertThat(posted.getStatus()).isEqualTo(401);
        assertThat(posted.getContentAsString()).doesNotContain(token).contains("/ical/***.ics");
        assertThat(shouted.getStatus()).isEqualTo(404);
        assertThat(shouted.getContentAsString()).doesNotContain(token);
    }

    @Test
    void sixty_reads_a_minute_from_one_address_then_a_refusal_that_names_no_token() throws Exception {
        String path = createLink(bobId, "{\"label\":\"iPhone\",\"sources\":[\"EVENT\"]}");
        String token = path.substring("/ical/".length(), path.length() - ".ics".length());
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get(path).with(from("203.0.113.5"))).andExpect(status().isOk());
        }

        MockHttpServletResponse limited = mockMvc.perform(get(path).with(from("203.0.113.5"))).andReturn().getResponse();

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).doesNotContain(token);
        mockMvc.perform(get(path).with(from("203.0.113.6"))).andExpect(status().isOk());
    }

    private static RequestPostProcessor from(String address) {
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }

    private String createLink(UUID userId, String body) throws Exception {
        String response = mockMvc.perform(post(links()).cookie(TestAccessTokens.cookieFor(userId)).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return URI.create(objectMapper.readTree(response).get("url").asText()).getPath();
    }

    private void createEvent(String title, String day) throws Exception {
        mockMvc.perform(post("/api/spaces/" + spaceId + "/calendar/events").cookie(TestAccessTokens.cookieFor(aliceId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"allDay\":true,\"startDate\":\"" + day + "\",\"endDate\":\"" + day
                    + "\",\"participantIds\":[]}"))
            .andExpect(status().isCreated());
    }

    private String links() {
        return "/api/spaces/" + spaceId + "/calendar/subscription-links";
    }

}
