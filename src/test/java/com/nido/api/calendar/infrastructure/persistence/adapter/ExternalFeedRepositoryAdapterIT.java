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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        spaceId = newSpace("Colocation");

        UserIdentityEntity user = new UserIdentityEntity();
        user.setUsername("alice-" + UUID.randomUUID());
        user.setEmail(UUID.randomUUID() + "@test.com");
        user.setRole(Role.USER);
        aliceId = users.saveAndFlush(user).getId();
    }

    @Autowired ExternalFeedRepository feeds;

    @AfterEach
    void cleanUp() {
        // The database is shared with every other test: only what this one made goes, its feeds with their spaces.
        jdbc.update("DELETE FROM calendar_external_feeds WHERE space_id = ANY(?)", (Object) madeSpaces.toArray(UUID[]::new));
        madeSpaces.forEach(spaces::deleteById);
        users.deleteById(aliceId);
    }

    private final List<UUID> madeSpaces = new ArrayList<>();

    private UUID newSpace(String name) {
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, name);
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        UUID id = spaces.saveAndFlush(space).getId();
        madeSpaces.add(id);
        return id;
    }

    private static final String ICS = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";

    @Test
    void a_followed_calendar_is_kept_sealed_its_content_apart() {
        Instant at = Instant.parse("2026-10-13T10:00:00Z");
        ExternalFeed feed = feeds.create(spaceId, "Vacances scolaires", URI.create("https://ecole.example/v.ics?token=secret"),
            "event-cyan", aliceId, "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", "\"v1\"", null, at);

        Map<String, Object> row = jdbc.queryForMap("SELECT name_encrypted, url_encrypted FROM calendar_external_feeds WHERE id = ?",
            feed.id());
        assertThat((String) row.get("name_encrypted")).startsWith("v3:").doesNotContain("Vacances");
        assertThat((String) row.get("url_encrypted")).doesNotContain("secret");
        assertThat(jdbc.queryForObject("SELECT content_encrypted FROM calendar_external_feed_contents WHERE feed_id = ?",
            String.class, feed.id())).startsWith("v3:");

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
        // Long before anything another test makes: the search spans every space, and nothing else is that old.
        Instant old = Instant.parse("1971-01-01T00:00:00Z");
        UUID third = feeds.create(spaceId, "Troisième", URI.create("https://c.example/c.ics"), null, aliceId, ICS, null, null, old.plusSeconds(30)).id();
        UUID first = feeds.create(spaceId, "Premier", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null, old).id();
        UUID second = feeds.create(spaceId, "Deuxième", URI.create("https://b.example/b.ics"), null, aliceId, ICS, null, null, old.plusSeconds(10)).id();
        feeds.create(spaceId, "Récent", URI.create("https://d.example/d.ics"), null, aliceId, ICS, null, null, old.plusSeconds(6 * 3600));
        Instant before = old.plusSeconds(3600);

        assertThat(feeds.findDueIds(before, 10)).containsExactly(first, second, third);
        assertThat(feeds.findDueIds(before, 2)).containsExactly(first, second);
    }

    @Test
    void calendars_last_tried_at_the_same_instant_come_in_the_order_of_their_ids() {
        Instant old = Instant.parse("1971-01-01T00:00:00Z");
        List<UUID> made = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            made.add(feeds.create(spaceId, "Calendrier " + i, URI.create("https://e.example/" + i + ".ics"), null, aliceId,
                ICS, null, null, old).id());
        }
        List<UUID> byId = made.stream().sorted(Comparator.comparing(UUID::toString)).toList();

        // Pages that never skip nor repeat a calendar, whatever the database's own order.
        assertThat(feeds.findDueIds(old.plusSeconds(1), 10)).containsExactlyElementsOf(byId);
        assertThat(feeds.findDueIds(old.plusSeconds(1), 3)).containsExactlyElementsOf(byId.subList(0, 3));
    }

    @Test
    void a_calendar_unchanged_keeps_its_content_and_forgets_its_last_error() {
        Instant fetched = Instant.parse("2026-10-13T00:00:00Z");
        ExternalFeed feed = feeds.create(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, "\"v1\"",
            "Tue, 13 Oct 2026 00:00:00 GMT", fetched);
        feeds.recordFailure(feed.id(), FeedError.http(503), fetched.plusSeconds(60));

        feeds.recordNotModified(feed.id(), fetched.plusSeconds(120));

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.lastError()).isNull();
        assertThat(after.lastAttemptAt()).isEqualTo(fetched.plusSeconds(120));
        assertThat(after.contentFetchedAt()).isEqualTo(fetched);
        assertThat(after.etag()).isEqualTo("\"v1\"");
        assertThat(feeds.loadContent(feed.id())).isEqualTo(ICS);
    }

    @Test
    void a_calendar_is_renamed_and_recoloured_its_name_sealed_again() {
        ExternalFeed feed = feeds.create(spaceId, "Club", URI.create("https://a.example/a.ics"), "event-cyan", aliceId, ICS,
            null, null, Instant.parse("2026-10-13T00:00:00Z"));

        feeds.update(feed.id(), "Club de foot", "event-brique");

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.name()).isEqualTo("Club de foot");
        assertThat(after.color()).isEqualTo("event-brique");
        assertThat(jdbc.queryForObject("SELECT name_encrypted FROM calendar_external_feeds WHERE id = ?", String.class, feed.id()))
            .startsWith("v3:").doesNotContain("foot");
    }

    @Test
    void a_calendar_no_longer_followed_takes_its_content_with_it() {
        ExternalFeed feed = feeds.create(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS,
            null, null, Instant.parse("2026-10-13T00:00:00Z"));

        feeds.delete(feed.id());

        assertThat(feeds.findById(feed.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_external_feed_contents WHERE feed_id = ?", Long.class,
            feed.id())).isZero();
    }

    @Test
    void each_space_counts_and_lists_its_own_calendars_only() {
        UUID elsewhere = newSpace("Ailleurs");
        Instant at = Instant.parse("2026-10-13T00:00:00Z");
        feeds.create(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null, at);
        feeds.create(spaceId, "École", URI.create("https://b.example/b.ics"), null, aliceId, ICS, null, null, at);
        feeds.create(elsewhere, "Mairie", URI.create("https://c.example/c.ics"), null, aliceId, ICS, null, null, at);

        assertThat(feeds.countBySpaceId(spaceId)).isEqualTo(2);
        assertThat(feeds.countBySpaceId(elsewhere)).isEqualTo(1);
        assertThat(feeds.findBySpaceId(spaceId)).extracting(ExternalFeed::name).containsExactlyInAnyOrder("Club", "École");
        assertThat(feeds.findBySpaceId(elsewhere)).extracting(ExternalFeed::name).containsExactly("Mairie");
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
