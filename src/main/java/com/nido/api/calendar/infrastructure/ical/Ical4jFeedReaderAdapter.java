package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.FeedCheck;
import com.nido.api.calendar.domain.model.FeedToShow;
import com.nido.api.calendar.domain.port.out.FeedOccurrenceReader;
import com.nido.api.calendar.domain.port.out.IcalFeedChecker;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Reads the calendars a space follows: whether a download is one, and their occurrences on a window. */
@org.springframework.stereotype.Component
public class Ical4jFeedReaderAdapter implements IcalFeedChecker, FeedOccurrenceReader {

    private static final Logger log = LoggerFactory.getLogger(Ical4jFeedReaderAdapter.class);

    /** At most 2 000 occurrences of one calendar in one read. */
    static final int MAX_OCCURRENCES = 2_000;

    /**
     * What all the followed calendars of one request may ask of ical4j together. A view or a dashboard reads them at every
     * load, so this is a fifth of what an import spends once: about half a second at worst.
     */
    static final long FEED_READ_BUDGET = 1_000_000;

    private final ParsedFeeds parsed = new ParsedFeeds();

    @Override
    public FeedCheck check(String content) {
        IcalSupport.Read read = IcalSupport.read(content);
        return new FeedCheck(read.calendar().getProperty("X-WR-CALNAME").map(Property::getValue).map(String::strip)
            .filter(name -> !name.isEmpty()), FeedWeight.of(content, read));
    }

    /**
     * Their rules expanded under one budget for all of them, EXDATE applied, every occurrence edited on its own shown in
     * its slot's place, nothing cancelled. An event whose rule cannot be expanded (or no longer affordable) is left out,
     * and the rest is shown. A calendar that cannot be read is left out, and remembered as such.
     */
    @Override
    public List<CalendarOccurrence> occurrencesOf(List<FeedToShow> feeds, Function<UUID, String> content, ZoneId zone,
                                                  Locale locale, LocalDate from, LocalDate to) {
        // One budget per request: the calendars kept are shared, the budget is not.
        BoundedExpansion expansion = new BoundedExpansion(zone, FEED_READ_BUDGET);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (FeedToShow feed : feeds) {
            ParsedFeeds.Parsed entry = parsed.of(feed, content);
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

    private List<CalendarOccurrence> expand(FeedToShow feed, ParsedFeeds.Parsed entry, BoundedExpansion expansion, ZoneId zone,
                                            Locale locale, LocalDate from, LocalDate to) {
        ZonedDateTime windowStart = from.atStartOfDay(zone);
        ZonedDateTime windowEnd = to.plusDays(1).atStartOfDay(zone);
        List<CalendarOccurrence> found = new ArrayList<>();
        for (ParsedFeeds.Group group : entry.groups) {
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

    private void readGroup(FeedToShow feed, ParsedFeeds.Parsed entry, ParsedFeeds.Group group, BoundedExpansion expansion,
                           ZoneId zone, Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd, LocalDate from,
                           LocalDate to, List<CalendarOccurrence> found) {
        List<VEvent> masters = IcalGroups.masters(group.events());
        // One version of a slot edited twice, as the import keeps: an occurrence is never shown twice.
        List<VEvent> overrides = IcalGroups.onePerSlot(IcalGroups.overrides(group.events()), zone, dropped -> { });
        String digest = group.digest();
        Set<LocalDateTime> replaced = IcalGroups.slotsTaken(overrides, zone);
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
            // An edited occurrence stands alone, with its own times, and is named by the slot it took over: that slot is
            // shown by nothing else, where its own new start may be another occurrence's.
            if (found.size() < MAX_OCCURRENCES && !IcalGroups.cancelled(override)) {
                try {
                    Optional<IcalTimes> times = IcalTimes.of(override, zone).filter(IcalTimes::fits);
                    if (times.isPresent() && overlaps(times.get(), from, to)) {
                        LocalDateTime slot = IcalGroups.slotOf(override, zone).orElse(times.get().start());
                        add(found, feed, digest, slot, IcalTexts.of(override, locale), times.get());
                    }
                } catch (RuntimeException unreadable) {
                    log.debug("Followed calendar {}: an edited occurrence left out ({})", feed.id(), unreadable.getClass().getSimpleName());
                }
            }
        }
    }

    private void readMaster(FeedToShow feed, String digest, VEvent master, Set<LocalDateTime> replaced, BoundedExpansion expansion,
                            ZoneId zone, Locale locale, ZonedDateTime windowStart, ZonedDateTime windowEnd, LocalDate from,
                            LocalDate to, List<CalendarOccurrence> found) {
        if (IcalGroups.cancelled(master)) {
            return;
        }
        // An event longer than a calendar read shows is one nido does not hold: the import sets it aside too.
        Optional<IcalTimes> read = IcalTimes.of(master, zone).filter(IcalTimes::fits);
        if (read.isEmpty()) {
            return;
        }
        IcalTimes times = read.get();
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
        shown.forEach(occurrence -> add(found, feed, digest, occurrence.start(), texts, occurrence));
    }

    private static boolean overlaps(IcalTimes times, LocalDate from, LocalDate to) {
        return !times.endDate().isBefore(from) && !times.startDate().isAfter(to);
    }

    private static void add(List<CalendarOccurrence> found, FeedToShow feed, String digest, LocalDateTime slot, IcalTexts texts,
                            IcalTimes times) {
        found.add(new CalendarOccurrence(CalendarSourceType.EXTERNAL, feed.id() + ":" + digest + ":" + slot,
            null, null, true, texts.title(), texts.description(), texts.location(), times.allDay(),
            times.startDate(), times.startTime(), times.endDate(), times.endTime(), feed.color(), List.of(), feed.id()));
    }
}
