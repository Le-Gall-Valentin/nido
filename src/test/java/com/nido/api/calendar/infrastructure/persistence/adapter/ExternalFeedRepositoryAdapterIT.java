package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.domain.port.out.ExternalFeedRepository;
import com.nido.api.identity.infrastructure.persistence.entity.UserIdentityEntity;
import com.nido.api.identity.infrastructure.persistence.repository.UserIdentityJpaRepository;
import com.nido.api.shared.model.Role;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTestConfig
class ExternalFeedRepositoryAdapterIT {

    @Autowired SpaceJpaRepository spaces;
    @Autowired UserIdentityJpaRepository users;
    @Autowired JdbcTemplate jdbc;

    private UUID spaceId;
    private UUID aliceId;

    @BeforeEach
    void setUp() {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Colocation");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaces.saveAndFlush(space).getId();

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@test.com");
        user.setRole(Role.USER);
        aliceId = users.saveAndFlush(user).getId();
    }

    @Autowired ExternalFeedRepository feeds;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM calendar_external_feeds WHERE space_id = ?", spaceId);
    }

    @Test
    void a_followed_calendar_is_kept_sealed_its_content_apart() {
        Instant at = Instant.parse("2026-10-13T10:00:00Z");
        ExternalFeed feed = feeds.create(spaceId, "Vacances scolaires", URI.create("https://ecole.example/v.ics?token=secret"),
            "event-cyan", aliceId, "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", "\"v1\"", null, at);

        Map<String, Object> row = jdbc.queryForMap("SELECT name_encrypted, url_encrypted FROM calendar_external_feeds");
        assertThat((String) row.get("name_encrypted")).startsWith("v3:").doesNotContain("Vacances");
        assertThat((String) row.get("url_encrypted")).doesNotContain("secret");
        assertThat(jdbc.queryForObject("SELECT content_encrypted FROM calendar_external_feed_contents", String.class)).startsWith("v3:");

        assertThat(feeds.findBySpaceId(spaceId)).singleElement().satisfies(found -> {
            assertThat(found.name()).isEqualTo("Vacances scolaires");
            assertThat(found.host()).isEqualTo("ecole.example");
            assertThat(found.etag()).isEqualTo("\"v1\"");
            assertThat(found.contentFetchedAt()).isEqualTo(at);
        });
        assertThat(feeds.loadContent(feed.id())).isEqualTo("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");
    }

    @Test
    void a_failure_keeps_the_last_calendar_and_says_why() {
        Instant first = Instant.parse("2026-10-13T10:00:00Z");
        ExternalFeed feed = feeds.create(spaceId, "Club", URI.create("https://club.example/c.ics"), null, aliceId,
            "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", null, null, first);

        feeds.recordFailure(feed.id(), FeedError.http(404), first.plusSeconds(3600));

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.lastError()).isEqualTo(FeedError.http(404));
        assertThat(after.contentFetchedAt()).isEqualTo(first);
        assertThat(after.lastAttemptAt()).isEqualTo(first.plusSeconds(3600));
        assertThat(feeds.loadContent(feed.id())).contains("VCALENDAR");

        feeds.recordSuccess(feed.id(), "BEGIN:VCALENDAR\r\nX-WR-CALNAME:Club\r\nEND:VCALENDAR\r\n", "\"v2\"", null, first.plusSeconds(7200));
        ExternalFeed recovered = feeds.findById(feed.id()).orElseThrow();
        assertThat(recovered.lastError()).isNull();
        assertThat(recovered.contentFetchedAt()).isEqualTo(first.plusSeconds(7200));
        assertThat(feeds.loadContent(feed.id())).contains("X-WR-CALNAME:Club");
    }

    @Test
    void the_calendars_not_tried_for_a_while_are_found_the_longest_unasked_first_and_no_more_than_the_limit() {
        String ics = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";
        Instant old = Instant.parse("2020-01-01T00:00:00Z");
        UUID third = feeds.create(spaceId, "Troisième", URI.create("https://c.example/c.ics"), null, aliceId, ics, null, null, old.plusSeconds(30)).id();
        UUID first = feeds.create(spaceId, "Premier", URI.create("https://a.example/a.ics"), null, aliceId, ics, null, null, old).id();
        UUID second = feeds.create(spaceId, "Deuxième", URI.create("https://b.example/b.ics"), null, aliceId, ics, null, null, old.plusSeconds(10)).id();
        feeds.create(spaceId, "Récent", URI.create("https://d.example/d.ics"), null, aliceId, ics, null, null, old.plusSeconds(6 * 3600));
        Instant before = old.plusSeconds(3600);

        assertThat(feeds.findDueIds(before, 10)).containsExactly(first, second, third);
        assertThat(feeds.findDueIds(before, 2)).containsExactly(first, second);
    }

    @Test
    void an_attempt_is_marked_without_touching_what_was_fetched() {
        Instant fetched = Instant.parse("2026-10-13T00:00:00Z");
        ExternalFeed feed = feeds.create(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId,
            "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", null, null, fetched);

        feeds.markAttempt(feed.id(), fetched.plusSeconds(60));

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.lastAttemptAt()).isEqualTo(fetched.plusSeconds(60));
        assertThat(after.contentFetchedAt()).isEqualTo(fetched);
        assertThat(after.lastError()).isNull();
    }
}
