package com.nido.api.calendar.infrastructure.ical;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.ExternalFeed;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.RecurrenceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Reads the calendars a space follows: whether a download is one, and their occurrences on a window. */
@org.springframework.stereotype.Component
public class Ical4jFeedReaderAdapter implements IcalFeedChecker {

    private static final Logger log = LoggerFactory.getLogger(Ical4jFeedReaderAdapter.class);

    /** At most 2 000 occurrences of one calendar in one read. */
    static final int MAX_OCCURRENCES = 2_000;

    /**
     * What a VTIMEZONE costs in the weight of a cached calendar. Each one whose TZID the JDK does not know holds an id of
     * ical4j's global pool (1 500 in all, for every document) for as long as its calendar is cached, and a document may
     * declare 50 of them. Weighed by file size alone, a few hostile calendars of a few lines each could exhaust the pool
     * for everyone; at this weight the whole cache holds about a thousand zones at most.
     */
    static final int ZONE_WEIGHT = 32_768;

    /** What is kept in memory: a calendar read once, weighed by its file and its zones. */
    private record Parsed(Calendar calendar, int weight) {}

    private record Key(UUID feedId, Instant fetchedAt) {}

    // Keyed by the download as well as the calendar: a calendar downloaded again is a new key, and the old one ages out.
    private final Cache<Key, Parsed> parsed = Caffeine.newBuilder()
        .maximumWeight(32L * 1024 * 1024)
        .weigher((Key key, Parsed value) -> value.weight())
        .build();

    @Override
    public Optional<String> calendarName(String content) {
        Calendar calendar = IcalSupport.parse(content);
        return calendar.getProperty("X-WR-CALNAME").map(Property::getValue).map(String::strip).filter(name -> !name.isEmpty());
    }

    static int weightOf(String text, Calendar calendar) {
        long zones = calendar.getComponents(Component.VTIMEZONE).size();
        return (int) Math.min(Integer.MAX_VALUE, text.length() + ZONE_WEIGHT * zones);
    }

    /**
     * The occurrences of a followed calendar overlapping {@code [from, to]}, in the space's zone: its rules expanded
     * under a budget, EXDATE applied, every occurrence edited on its own shown in its slot's place, nothing cancelled.
     * An event whose rule cannot be expanded is left out, and the rest is shown.
     */
    public List<CalendarOccurrence> occurrencesOf(ExternalFeed feed, Supplier<String> content, ZoneId zone, Locale locale,
                                                  LocalDate from, LocalDate to) {
        Parsed entry = parsed.get(new Key(feed.id(), feed.contentFetchedAt()), key -> {
            String text = content.get();
            Calendar calendar = IcalSupport.parse(text);
            return new Parsed(calendar, weightOf(text, calendar));
        });
        // Two requests may read the same calendar at once. Reading it is not entirely read-only: ical4j's Recur builds its
        // BY-rule objects into plain fields on each expansion. The objects are equal each time, so it would likely be
        // harmless, but it is not safe by contract: readers of one calendar take turns (an expansion is budgeted, and
        // readers of other calendars are not held).
        synchronized (entry) {
            return expand(feed, entry.calendar(), zone, locale, from, to);
        }
    }

    private List<CalendarOccurrence> expand(ExternalFeed feed, Calendar calendar, ZoneId zone, Locale locale,
                                            LocalDate from, LocalDate to) {
        // One budget per read: the cached calendar is shared, the budget is not.
        BoundedExpansion expansion = new BoundedExpansion(zone);
        ZonedDateTime windowStart = from.atStartOfDay(zone);
        ZonedDateTime windowEnd = to.plusDays(1).atStartOfDay(zone);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (Map.Entry<String, List<VEvent>> group : byUid(calendar).entrySet()) {
            if (found.size() >= MAX_OCCURRENCES) {
                break;
            }
            try {
                readGroup(feed, group, expansion, zone, locale, windowStart, windowEnd, from, to, found);
            } catch (BoundedExpansion.Refused refused) {
                log.debug("Followed calendar {}: an event left out ({})", feed.id(), refused.detail());
            } catch (RuntimeException unreadable) {
                // The one broad catch of the read: a third-party library expands rules from an untrusted file, and what
                // it throws on one event must cost that event, not the calendar.
                log.debug("Followed calendar {}: an event left out ({})", feed.id(), unreadable.getClass().getSimpleName());
            }
        }
        return found.size() > MAX_OCCURRENCES ? List.copyOf(found.subList(0, MAX_OCCURRENCES)) : found;
    }

    private void readGroup(ExternalFeed feed, Map.Entry<String, List<VEvent>> group, BoundedExpansion expansion, ZoneId zone,
                           Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd, LocalDate from, LocalDate to,
                           List<CalendarOccurrence> found) {
        Set<LocalDateTime> replaced = new HashSet<>();
        for (VEvent event : group.getValue()) {
            event.<RecurrenceId<Temporal>>getProperty(Property.RECURRENCE_ID)
                .map(id -> IcalSupport.toLocal(id.getDate(), zone)).ifPresent(replaced::add);
        }
        for (VEvent event : group.getValue()) {
            if (found.size() >= MAX_OCCURRENCES) {
                return;
            }
            // Each event on its own: one that cannot be read does not take its siblings with it.
            try {
                readEvent(feed, group.getKey(), event, replaced, expansion, zone, locale, windowStart, windowEnd, from, to, found);
            } catch (BoundedExpansion.Refused refused) {
                log.debug("Followed calendar {}: an event left out ({})", feed.id(), refused.detail());
            } catch (RuntimeException unreadable) {
                log.debug("Followed calendar {}: an event left out ({})", feed.id(), unreadable.getClass().getSimpleName());
            }
        }
    }

    private void readEvent(ExternalFeed feed, String uid, VEvent event, Set<LocalDateTime> replaced, BoundedExpansion expansion,
                           ZoneId zone, Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd,
                           LocalDate from, LocalDate to, List<CalendarOccurrence> found) {
        if (Ical4jImportReaderAdapter.cancelled(event)) {
            return;
        }
        Optional<IcalTimes> read = IcalTimes.of(event, zone);
        if (read.isEmpty()) {
            return;
        }
        IcalTimes times = read.get();
        IcalTexts texts = IcalTexts.of(event, locale);
        if (event.getProperty(Property.RECURRENCE_ID).isPresent()) {
            // An edited occurrence stands alone, with its own times.
            add(found, feed, uid, texts, times, from, to);
            return;
        }
        // From a duration before the window, so an occurrence that began before it and runs into it is found.
        ZonedDateTime askedFrom = windowStart.minus(Duration.between(times.start(), times.end()));
        for (LocalDateTime start : expansion.occurrences(event, times, askedFrom, windowEnd, MAX_OCCURRENCES - found.size())) {
            if (!replaced.contains(start)) {
                add(found, feed, uid, texts, times.at(start), from, to);
            }
        }
    }

    /** The events of a calendar by UID: a rule and the occurrences it edited on their own. No UID, a group alone. */
    private static Map<String, List<VEvent>> byUid(Calendar calendar) {
        Map<String, List<VEvent>> byUid = new LinkedHashMap<>();
        int anonymous = 0;
        for (VEvent event : calendar.<VEvent>getComponents(Component.VEVENT)) {
            String uid = event.getProperty(Property.UID).map(Property::getValue).orElse(null);
            byUid.computeIfAbsent(uid != null ? uid : "\0" + anonymous++, key -> new ArrayList<>()).add(event);
        }
        return byUid;
    }

    private static void add(List<CalendarOccurrence> found, ExternalFeed feed, String uid, IcalTexts texts,
                            IcalTimes times, LocalDate from, LocalDate to) {
        if (times.endDate().isBefore(from) || times.startDate().isAfter(to)) {
            return;
        }
        found.add(new CalendarOccurrence(CalendarSourceType.EXTERNAL, feed.id() + ":" + uid + ":" + times.start(),
            null, null, true, texts.title(), texts.description(), texts.location(), times.allDay(),
            times.startDate(), times.startTime(), times.endDate(), times.endTime(), feed.color(), List.of(), feed.id()));
    }
}
