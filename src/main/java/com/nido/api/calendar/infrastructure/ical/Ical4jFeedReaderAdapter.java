package com.nido.api.calendar.infrastructure.ical;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentContainer;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/** Reads the calendars a space follows: whether a download is one, and their occurrences on a window. */
@org.springframework.stereotype.Component
public class Ical4jFeedReaderAdapter implements IcalFeedChecker {

    private static final Logger log = LoggerFactory.getLogger(Ical4jFeedReaderAdapter.class);

    /** At most 2 000 occurrences of one calendar in one read. */
    static final int MAX_OCCURRENCES = 2_000;

    /**
     * What all the followed calendars of one request may ask of ical4j together. A view or a dashboard reads them at every
     * load, inside a transaction that holds a connection, so this is a fifth of what an import spends once: about half a
     * second at worst.
     */
    static final long FEED_READ_BUDGET = 1_000_000;

    /**
     * What a VTIMEZONE costs in the weight of a cached calendar. Each one whose TZID the JDK does not know holds an id of
     * ical4j's global pool (1 500 in all, for every document) for as long as its calendar is cached, and a document may
     * declare 50 of them. Weighed by file size alone, a few hostile calendars of a few lines each could exhaust the pool
     * for everyone; at this weight the whole cache holds about a thousand zones at most.
     */
    static final int ZONE_WEIGHT = 32_768;

    /**
     * What a property costs in the weight of a cached calendar. The text of a file is not what it holds in memory: measured
     * on 5 MB, a realistic calendar retains 39 MB of heap (8 times), and one made of lines of {@code X-A:1} retains 142 MB
     * (28 times). Counting each property brings the second to its real weight and leaves the first within a factor of two.
     */
    static final int PROPERTY_WEIGHT = 128;

    /**
     * The heaviest calendar kept, and the weight all of them share: an entry above the bound of the cache is evicted as
     * soon as it is put, and read again from the database at every view. So a calendar above the ceiling is refused when it
     * is downloaded, and the bound is four times the ceiling: the heaviest feeds accepted fit together.
     */
    static final int ENTRY_CEILING = FeedCheck.MAX_WEIGHT;
    static final long CACHE_WEIGHT = 4L * ENTRY_CEILING;

    /** How long an unreadable calendar is remembered as such: long enough to spare every view, short enough for a database that had a bad moment. */
    private static final Duration FAILURE_MEMORY = Duration.ofMinutes(10);

    /** A UID's events, with the digest that names them in the ids of their occurrences. */
    private record Group(String digest, List<VEvent> events) {}

    /** What is kept in memory: a calendar read once, its events grouped, and the events found too heavy to expand. */
    private static final class Parsed {
        final List<Group> groups;
        final int weight;
        /** Read and written only while holding the entry: by identity, an event's equality is its whole content. */
        final Set<VEvent> refused = Collections.newSetFromMap(new IdentityHashMap<>());

        Parsed(List<Group> groups, int weight) {
            this.groups = groups;
            this.weight = weight;
        }
    }

    private record Key(UUID feedId, Instant fetchedAt) {}

    // Keyed by the download as well as the calendar: a calendar downloaded again is a new key, and the old one ages out.
    private final Cache<Key, Parsed> parsed = Caffeine.newBuilder()
        .maximumWeight(CACHE_WEIGHT)
        .weigher((Key key, Parsed value) -> value.weight)
        .build();

    private final Cache<Key, Boolean> unreadable = Caffeine.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(FAILURE_MEMORY)
        .build();

    /** Told each time an event is handed to the expansion: lets a test see what is not tried again. */
    private final Runnable onExpansion;

    public Ical4jFeedReaderAdapter() {
        this(() -> {});
    }

    Ical4jFeedReaderAdapter(Runnable onExpansion) {
        this.onExpansion = onExpansion;
    }

    @Override
    public FeedCheck check(String content) {
        Calendar calendar = IcalSupport.parse(content);
        return new FeedCheck(calendar.getProperty("X-WR-CALNAME").map(Property::getValue).map(String::strip)
            .filter(name -> !name.isEmpty()), weightOf(content, calendar));
    }

    static int weightOf(String text, Calendar calendar) {
        long zones = 0;
        long properties = calendar.getProperties().size();
        Deque<Component> pending = new ArrayDeque<>(calendar.getComponents());
        while (!pending.isEmpty()) {
            Component component = pending.pop();
            if (Component.VTIMEZONE.equals(component.getName())) {
                zones++;
            }
            properties += component.getProperties().size();
            if (component instanceof ComponentContainer<?> container) {
                pending.addAll(container.getComponents());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, text.length() + PROPERTY_WEIGHT * properties + ZONE_WEIGHT * zones);
    }

    /** One calendar: the same as {@link #occurrencesOf(List, Function, ZoneId, Locale, LocalDate, LocalDate)} for a single one. */
    public List<CalendarOccurrence> occurrencesOf(ExternalFeed feed, Supplier<String> content, ZoneId zone, Locale locale,
                                                  LocalDate from, LocalDate to) {
        return occurrencesOf(List.of(feed), ignored -> content.get(), zone, locale, from, to);
    }

    /**
     * The occurrences of the followed calendars overlapping {@code [from, to]}, in the space's zone: their rules expanded
     * under one budget for all of them, EXDATE applied, every occurrence edited on its own shown in its slot's place,
     * nothing cancelled. An event whose rule cannot be expanded (or no longer affordable) is left out, and the rest is
     * shown. A calendar that cannot be read is left out, and remembered as such.
     */
    public List<CalendarOccurrence> occurrencesOf(List<ExternalFeed> feeds, Function<ExternalFeed, String> content, ZoneId zone,
                                                  Locale locale, LocalDate from, LocalDate to) {
        // One budget per request: the cached calendars are shared, the budget is not.
        BoundedExpansion expansion = new BoundedExpansion(zone, FEED_READ_BUDGET);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (ExternalFeed feed : feeds) {
            Parsed entry = entryOf(feed, content);
            if (entry == null) {
                continue;
            }
            // Two requests may read the same calendar at once. Reading it is not entirely read-only: ical4j's Recur builds
            // its BY-rule objects into plain fields on each expansion. The objects are equal each time, so it would likely
            // be harmless, but it is not safe by contract: readers of one calendar take turns (an expansion is budgeted,
            // and readers of other calendars are not held).
            synchronized (entry) {
                found.addAll(expand(feed, entry, expansion, zone, locale, from, to));
            }
        }
        return found;
    }

    /** The calendar in memory, read and parsed outside the cache if it is not; null when it cannot be. */
    private Parsed entryOf(ExternalFeed feed, Function<ExternalFeed, String> content) {
        Key key = new Key(feed.id(), feed.contentFetchedAt());
        Parsed entry = parsed.getIfPresent(key);
        if (entry != null) {
            return entry;
        }
        if (unreadable.getIfPresent(key) != null) {
            return null;
        }
        // Loaded here and not inside the cache's own computation, which would hold a lock of its table for as long as a
        // database read, an unsealing and a parse of five megabytes take. Two requests may both load: one is kept.
        try {
            String text = content.apply(feed);
            Calendar calendar = IcalSupport.parse(text);
            int weight = weightOf(text, calendar);
            if (weight > ENTRY_CEILING) {
                // Stored before the ceiling, or past it all the same: never kept, so never parsed again at every view.
                unreadable.put(key, Boolean.TRUE);
                log.warn("Followed calendar {} skipped: too heavy to keep in memory", feed.id());
                return null;
            }
            Parsed loaded = new Parsed(groupsOf(calendar), weight);
            Parsed already = parsed.asMap().putIfAbsent(key, loaded);
            return already != null ? already : loaded;
        } catch (RuntimeException failed) {
            // The feed id only: the address often holds a secret. Whatever a third-party parser or the unsealing throws
            // on one calendar must not take the view down, and is said once, not at every view.
            unreadable.put(key, Boolean.TRUE);
            log.warn("Followed calendar {} skipped: {}", feed.id(), failed.getClass().getSimpleName());
            return null;
        }
    }

    private static List<Group> groupsOf(Calendar calendar) {
        List<Group> groups = new ArrayList<>();
        // A short stable digest, never the UID: it is untrusted, as long as the file, and would be repeated in every id.
        IcalGroups.byUid(calendar.<VEvent>getComponents(Component.VEVENT)).forEach((uid, events) ->
            groups.add(new Group(UUID.nameUUIDFromBytes(uid.getBytes(StandardCharsets.UTF_8)).toString(), events)));
        return groups;
    }

    private List<CalendarOccurrence> expand(ExternalFeed feed, Parsed entry, BoundedExpansion expansion, ZoneId zone,
                                            Locale locale, LocalDate from, LocalDate to) {
        ZonedDateTime windowStart = from.atStartOfDay(zone);
        ZonedDateTime windowEnd = to.plusDays(1).atStartOfDay(zone);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (Group group : entry.groups) {
            if (found.size() >= MAX_OCCURRENCES) {
                break;
            }
            try {
                readGroup(feed, entry, group, expansion, zone, locale, windowStart, windowEnd, from, to, found);
            } catch (RuntimeException unreadable) {
                // What a third-party library throws on the shape of a group (a date it cannot reach) costs that group.
                log.debug("Followed calendar {}: a group of events left out ({})", feed.id(), unreadable.getClass().getSimpleName());
            }
        }
        return found;
    }

    private void readGroup(ExternalFeed feed, Parsed entry, Group group, BoundedExpansion expansion,
                           ZoneId zone, Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd, LocalDate from,
                           LocalDate to, List<CalendarOccurrence> found) {
        List<VEvent> masters = IcalGroups.masters(group.events());
        // One version of a slot edited twice, as the import keeps: an occurrence is never shown twice.
        List<VEvent> overrides = IcalGroups.onePerSlot(IcalGroups.overrides(group.events()), zone, dropped -> { });
        String digest = group.digest();
        Set<LocalDateTime> replaced = new HashSet<>();
        for (VEvent override : overrides) {
            override.<RecurrenceId<Temporal>>getProperty(Property.RECURRENCE_ID)
                .map(id -> IcalSupport.toLocal(id.getDate(), zone)).ifPresent(replaced::add);
        }
        // Only the first master of a UID stands: a followed calendar has no report to say the others were set aside.
        if (!masters.isEmpty()) {
            VEvent master = masters.getFirst();
            if (!entry.refused.contains(master)) {
                try {
                    readMaster(feed, digest, master, replaced, expansion, zone, locale, windowStart, windowEnd, from, to, found);
                } catch (BoundedExpansion.Refused refused) {
                    // Remembered only when no window would have it: a bill grows with the window, and the next read may be shorter.
                    if (refused.ofTheRule()) {
                        entry.refused.add(master);
                    }
                    log.debug("Followed calendar {}: an event left out ({})", feed.id(), refused.detail());
                } catch (RuntimeException unreadable) {
                    log.debug("Followed calendar {}: an event left out ({})", feed.id(), unreadable.getClass().getSimpleName());
                }
            }
        }
        for (VEvent override : overrides) {
            // An edited occurrence stands alone, with its own times.
            if (found.size() < MAX_OCCURRENCES && !Ical4jImportReaderAdapter.cancelled(override)) {
                try {
                    Optional<IcalTimes> times = IcalTimes.of(override, zone);
                    if (times.isPresent() && overlaps(times.get(), from, to)) {
                        add(found, feed, digest, IcalTexts.of(override, locale), times.get());
                    }
                } catch (RuntimeException unreadable) {
                    log.debug("Followed calendar {}: an edited occurrence left out ({})", feed.id(), unreadable.getClass().getSimpleName());
                }
            }
        }
    }

    private void readMaster(ExternalFeed feed, String digest, VEvent master, Set<LocalDateTime> replaced, BoundedExpansion expansion,
                            ZoneId zone, Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd, LocalDate from,
                            LocalDate to, List<CalendarOccurrence> found) {
        if (Ical4jImportReaderAdapter.cancelled(master)) {
            return;
        }
        Optional<IcalTimes> read = IcalTimes.of(master, zone);
        if (read.isEmpty()) {
            return;
        }
        IcalTimes times = read.get();
        onExpansion.run();
        List<IcalTimes> shown = new ArrayList<>();
        for (LocalDateTime start : expansion.occurrencesOverlapping(master, times, windowStart, windowEnd, MAX_OCCURRENCES - found.size())) {
            IcalTimes occurrence = times.at(start);
            if (!replaced.contains(start) && overlaps(occurrence, from, to)) {
                shown.add(occurrence);
            }
        }
        if (shown.isEmpty()) {
            return;
        }
        // The texts (and the markup stripped from them) only for an event that shows in the window.
        IcalTexts texts = IcalTexts.of(master, locale);
        shown.forEach(occurrence -> add(found, feed, digest, texts, occurrence));
    }

    private static boolean overlaps(IcalTimes times, LocalDate from, LocalDate to) {
        return !times.endDate().isBefore(from) && !times.startDate().isAfter(to);
    }

    private static void add(List<CalendarOccurrence> found, ExternalFeed feed, String digest, IcalTexts texts, IcalTimes times) {
        found.add(new CalendarOccurrence(CalendarSourceType.EXTERNAL, feed.id() + ":" + digest + ":" + times.start(),
            null, null, true, texts.title(), texts.description(), texts.location(), times.allDay(),
            times.startDate(), times.startTime(), times.endDate(), times.endTime(), feed.color(), List.of(), feed.id()));
    }
}
