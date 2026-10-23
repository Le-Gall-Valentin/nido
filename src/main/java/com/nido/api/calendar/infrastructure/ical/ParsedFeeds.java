package com.nido.api.calendar.infrastructure.ical;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.model.FeedToShow;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * The followed calendars kept in memory, read and grouped once: one entry a calendar, replaced at its first read after it
 * was downloaded again, and let go when no view has read it for a while. Weighed by what each holds (FeedWeight): the
 * heaviest calendar followed (FeedCheck) takes a fifth of the whole, and a space following more of those than fit has the
 * others read again from the database at its views. A calendar that cannot be read is remembered as such for a while, to
 * spare every view.
 */
final class ParsedFeeds {

    private static final Logger log = LoggerFactory.getLogger(ParsedFeeds.class);

    /** What all the calendars kept weigh together: about what they hold of the heap. */
    static final long CACHE_WEIGHT = 5L * FeedCheck.MAX_WEIGHT;

    /** How long a calendar no view reads stays: one removed, or of a space nobody looks at, is let go. */
    private static final Duration UNREAD_FOR = Duration.ofHours(6);

    /** How long an unreadable calendar is remembered as such: long enough to spare every view, short enough for a database that had a bad moment. */
    private static final Duration FAILURE_MEMORY = Duration.ofMinutes(10);

    /** A UID's events, with the digest that names them in the ids of their occurrences. */
    record Group(String digest, List<VEvent> events) {}

    /** A calendar read once, as downloaded at {@code fetchedAt}: its events grouped, and those found too heavy to expand. */
    static final class Parsed {
        final Instant fetchedAt;
        final List<Group> groups;
        final int weight;
        /** Read and written only while holding the entry: by identity, an event's equality is its whole content. */
        final Set<VEvent> refused = Collections.newSetFromMap(new IdentityHashMap<>());

        Parsed(Instant fetchedAt, List<Group> groups, int weight) {
            this.fetchedAt = fetchedAt;
            this.groups = groups;
            this.weight = weight;
        }
    }

    /** One download of one calendar. */
    private record Download(UUID feedId, Instant fetchedAt) {}

    private final Cache<UUID, Parsed> parsed = Caffeine.newBuilder()
        .maximumWeight(CACHE_WEIGHT)
        .weigher((UUID id, Parsed value) -> value.weight)
        .expireAfterAccess(UNREAD_FOR)
        .build();

    private final Cache<Download, Boolean> unreadable = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(FAILURE_MEMORY)
        .build();

    /** The calendar as last downloaded, read and parsed outside the cache if it is not kept; null when it cannot be. */
    Parsed of(FeedToShow feed, Function<UUID, String> content) {
        Parsed kept = parsed.getIfPresent(feed.id());
        if (kept != null && kept.fetchedAt.equals(feed.contentFetchedAt())) {
            return kept;
        }
        Download download = new Download(feed.id(), feed.contentFetchedAt());
        if (unreadable.getIfPresent(download) != null) {
            return null;
        }
        // Loaded here and not inside the cache's own computation, which would hold a lock of its table for as long as a
        // database read, an unsealing and a parse of five megabytes take. Two requests may both load: the later is kept.
        try {
            String text = content.apply(feed.id());
            IcalSupport.Read read = IcalSupport.read(text);
            int weight = FeedWeight.of(text, read);
            if (weight > FeedCheck.MAX_WEIGHT) {
                // Stored before the ceiling, or past it all the same: never kept, so never parsed again at every view.
                unreadable.put(download, Boolean.TRUE);
                parsed.invalidate(feed.id());
                log.warn("Followed calendar {} skipped: too heavy to keep in memory", feed.id());
                return null;
            }
            Parsed loaded = new Parsed(feed.contentFetchedAt(), groupsOf(read), weight);
            parsed.put(feed.id(), loaded);
            return loaded;
        } catch (RuntimeException failed) {
            // The feed id only: the address often holds a secret. Whatever a third-party parser or the unsealing throws
            // on one calendar must not take the view down, and is said once, not at every view.
            unreadable.put(download, Boolean.TRUE);
            log.warn("Followed calendar {} skipped: {}", feed.id(), failed.getClass().getSimpleName());
            return null;
        }
    }

    private static List<Group> groupsOf(IcalSupport.Read read) {
        List<Group> groups = new ArrayList<>();
        // A short stable digest, never the UID: it is untrusted, as long as the file, and would be repeated in every id.
        IcalGroups.byUid(read.calendar().<VEvent>getComponents(Component.VEVENT)).forEach((uid, events) -> {
            // A property nido reads did not parse: the event is left out, as the import sets it aside.
            if (!IcalGroups.unreadable(events)) {
                groups.add(new Group(UUID.nameUUIDFromBytes(uid.getBytes(StandardCharsets.UTF_8)).toString(), events));
            }
        });
        return groups;
    }
}
