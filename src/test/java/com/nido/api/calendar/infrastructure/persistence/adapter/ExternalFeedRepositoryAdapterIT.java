package com.nido.api.calendar.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedError;
import com.nido.api.calendar.infrastructure.persistence.entity.CalendarExternalFeedEntity;
import com.nido.api.calendar.infrastructure.persistence.repository.CalendarExternalFeedJpaRepository;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Autowired ExternalFeedRepositoryAdapter feeds;
    @Autowired CalendarExternalFeedJpaRepository rows;
    @Autowired PlatformTransactionManager transactions;

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

    /** Follows a calendar in a space far from any limit. */
    private ExternalFeed follow(UUID space, String name, URI url, String color, UUID by, String content, String etag,
                                String lastModified, Instant fetchedAt) {
        return feeds.createUnlessFollowing(100, space, name, url, color, by, content, etag, lastModified, fetchedAt).orElseThrow();
    }

    @Test
    void a_space_following_as_many_as_it_may_follows_no_more() {
        Instant at = Instant.parse("2026-10-13T00:00:00Z");
        assertThat(feeds.createUnlessFollowing(2, spaceId, "Un", URI.create("https://a.example/a.ics"), null, aliceId, ICS,
            null, null, at)).isPresent();
        assertThat(feeds.createUnlessFollowing(2, spaceId, "Deux", URI.create("https://b.example/b.ics"), null, aliceId, ICS,
            null, null, at)).isPresent();

        assertThat(feeds.createUnlessFollowing(2, spaceId, "Trois", URI.create("https://c.example/c.ics"), null, aliceId, ICS,
            null, null, at)).isEmpty();
        assertThat(feeds.countBySpaceId(spaceId)).isEqualTo(2);
    }

    @Test
    void two_follows_at_once_never_pass_the_limit_together() throws Exception {
        Instant at = Instant.parse("2026-10-13T00:00:00Z");
        CyclicBarrier together = new CyclicBarrier(2);
        Callable<Boolean> follow = () -> {
            together.await();
            return feeds.createUnlessFollowing(1, spaceId, "Un", URI.create("https://a.example/a.ics"), null, aliceId, ICS,
                null, null, at).isPresent();
        };
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> both = threads.invokeAll(List.of(follow, follow));
            assertThat(List.of(both.get(0).get(), both.get(1).get())).containsExactlyInAnyOrder(true, false);
        } finally {
            threads.shutdown();
        }
        assertThat(feeds.countBySpaceId(spaceId)).isEqualTo(1);
    }

    @Test
    void of_two_askings_within_the_minute_one_only_is_told_to_go_on() {
        Instant fetched = Instant.parse("2026-10-13T00:00:00Z");
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null,
            fetched);
        Instant now = fetched.plusSeconds(3600);

        assertThat(feeds.claimAttempt(feed.id(), now, now.minusSeconds(60))).isTrue();
        assertThat(feeds.claimAttempt(feed.id(), now.plusSeconds(5), now.plusSeconds(5).minusSeconds(60))).isFalse();
        assertThat(feeds.findById(feed.id()).orElseThrow().lastAttemptAt()).isEqualTo(now);
        assertThat(feeds.claimAttempt(UUID.randomUUID(), now, now)).as("a calendar gone").isFalse();
    }

    @Test
    void a_rename_keeps_what_a_refresh_wrote_since_it_read_the_calendar() {
        Instant fetched = Instant.parse("2026-10-13T00:00:00Z");
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null,
            fetched);
        Instant refreshedAt = fetched.plusSeconds(3600);

        // The rename has read the calendar; a refresh writes and commits on another connection; the rename then writes.
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            CalendarExternalFeedEntity read = rows.findById(feed.id()).orElseThrow();
            CompletableFuture.runAsync(() -> jdbc.update(
                "UPDATE calendar_external_feeds SET content_fetched_at = ?, last_attempt_at = ?, etag = 'v2' WHERE id = ?",
                Timestamp.from(refreshedAt), Timestamp.from(refreshedAt), feed.id())).join();
            read.setColor("event-brique");
            rows.saveAndFlush(read);
        });

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.color()).isEqualTo("event-brique");
        assertThat(after.contentFetchedAt()).isEqualTo(refreshedAt);
        assertThat(after.etag()).isEqualTo("v2");
    }

    @Test
    void a_followed_calendar_is_kept_sealed_its_content_apart() {
        Instant at = Instant.parse("2026-10-13T10:00:00Z");
        ExternalFeed feed = follow(spaceId, "Vacances scolaires", URI.create("https://ecole.example/v.ics?token=secret"),
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
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://club.example/c.ics"), null, aliceId,
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
        UUID third = follow(spaceId, "Troisième", URI.create("https://c.example/c.ics"), null, aliceId, ICS, null, null, old.plusSeconds(30)).id();
        UUID first = follow(spaceId, "Premier", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null, old).id();
        UUID second = follow(spaceId, "Deuxième", URI.create("https://b.example/b.ics"), null, aliceId, ICS, null, null, old.plusSeconds(10)).id();
        follow(spaceId, "Récent", URI.create("https://d.example/d.ics"), null, aliceId, ICS, null, null, old.plusSeconds(6 * 3600));
        Instant before = old.plusSeconds(3600);

        assertThat(feeds.findDueIds(before, 10)).containsExactly(first, second, third);
        assertThat(feeds.findDueIds(before, 2)).containsExactly(first, second);
    }

    @Test
    void calendars_last_tried_at_the_same_instant_come_in_the_order_of_their_ids() {
        Instant old = Instant.parse("1971-01-01T00:00:00Z");
        List<UUID> made = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            made.add(follow(spaceId, "Calendrier " + i, URI.create("https://e.example/" + i + ".ics"), null, aliceId,
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
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, "\"v1\"",
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
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), "event-cyan", aliceId, ICS,
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
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS,
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
        follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId, ICS, null, null, at);
        follow(spaceId, "École", URI.create("https://b.example/b.ics"), null, aliceId, ICS, null, null, at);
        follow(elsewhere, "Mairie", URI.create("https://c.example/c.ics"), null, aliceId, ICS, null, null, at);

        assertThat(feeds.countBySpaceId(spaceId)).isEqualTo(2);
        assertThat(feeds.countBySpaceId(elsewhere)).isEqualTo(1);
        assertThat(feeds.findBySpaceId(spaceId)).extracting(ExternalFeed::name).containsExactlyInAnyOrder("Club", "École");
        assertThat(feeds.findBySpaceId(elsewhere)).extracting(ExternalFeed::name).containsExactly("Mairie");
    }

    @Test
    void an_attempt_is_marked_without_touching_what_was_fetched() {
        Instant fetched = Instant.parse("2026-10-13T00:00:00Z");
        ExternalFeed feed = follow(spaceId, "Club", URI.create("https://a.example/a.ics"), null, aliceId,
            "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n", null, null, fetched);

        feeds.markAttempt(feed.id(), fetched.plusSeconds(60));

        ExternalFeed after = feeds.findById(feed.id()).orElseThrow();
        assertThat(after.lastAttemptAt()).isEqualTo(fetched.plusSeconds(60));
        assertThat(after.contentFetchedAt()).isEqualTo(fetched);
        assertThat(after.lastError()).isNull();
    }
}
