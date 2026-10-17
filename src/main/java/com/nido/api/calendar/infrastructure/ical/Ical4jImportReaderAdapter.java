package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.EventScheduleValidator;
import com.nido.api.calendar.domain.model.ImportGroup;
import com.nido.api.calendar.domain.model.ImportNote;
import com.nido.api.calendar.domain.model.ImportNoteKind;
import com.nido.api.calendar.domain.model.ImportReading;
import com.nido.api.calendar.domain.model.ImportedEvent;
import com.nido.api.calendar.domain.port.out.IcalImportReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Reads an iCalendar file into what a space can hold — tolerantly (see IcalSupport), placing every time in the space's
 * zone, grouping the events by UID (a series and the occurrences it edited on their own), and saying what it set aside.
 */
@org.springframework.stereotype.Component
public class Ical4jImportReaderAdapter implements IcalImportReader {

    static final int MAX_EVENTS = 5_000;
    private static final List<String> SET_ASIDE = List.of(Component.VTODO, Component.VJOURNAL, Component.VFREEBUSY);

    @Override
    public ImportReading read(String content, ZoneId zone, LocalDate today, Locale locale) {
        Calendar calendar = IcalSupport.parse(content);
        List<VEvent> vevents = calendar.getComponents(Component.VEVENT);
        if (vevents.size() > MAX_EVENTS) {
            throw new CalendarException.ImportTooLarge("more than " + MAX_EVENTS + " events");
        }
        List<ImportNote> notes = new ArrayList<>();
        int[] ignored = {0};
        for (String name : SET_ASIDE) {
            for (CalendarComponent other : calendar.<CalendarComponent>getComponents(name)) {
                ignored[0]++;
                notes.add(new ImportNote(ImportNoteKind.IGNORED, IcalTexts.titleOf(other), name));
            }
        }
        Map<String, List<VEvent>> byUid = new LinkedHashMap<>();
        int anonymous = 0;
        for (VEvent event : vevents) {
            String uid = event.getProperty(Property.UID).map(Property::getValue).map(String::strip).filter(s -> !s.isEmpty()).orElse(null);
            // An event with no UID stands alone: a key no UID can be.
            byUid.computeIfAbsent(uid != null ? uid : "\0" + anonymous++, key -> new ArrayList<>()).add(event);
        }
        RecurringImport recurring = new RecurringImport(zone, today, locale, new BoundedExpansion(zone));
        List<ImportGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<VEvent>> entry : byUid.entrySet()) {
            String uid = entry.getKey().startsWith("\0") ? null : entry.getKey();
            group(uid, entry.getValue(), zone, locale, recurring, notes, ignored).ifPresent(groups::add);
        }
        return new ImportReading(groups, ignored[0], notes);
    }

    /** What one UID becomes; counts in {@code ignored} and notes in {@code notes} what it sets aside. */
    private static Optional<ImportGroup> group(String uid, List<VEvent> events, ZoneId zone, Locale locale,
                                               RecurringImport recurring, List<ImportNote> notes, int[] ignored) {
        List<VEvent> masters = events.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isEmpty()).toList();
        List<VEvent> overrides = events.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isPresent()).toList();
        if (masters.isEmpty()) {
            // Edited occurrences of a series the file does not hold: each is an event of its own.
            List<ImportedEvent> orphans = new ArrayList<>();
            for (VEvent override : overrides) {
                standalone(override, zone, locale, notes, ignored).ifPresent(orphans::add);
            }
            return orphans.isEmpty() ? Optional.empty() : Optional.of(new ImportGroup(uid, orphans, null, false, 0));
        }
        VEvent master = masters.getFirst();
        // A second master of one UID is not something nido can say: only the first stands.
        masters.stream().skip(1).forEach(extra -> {
            ignored[0]++;
            notes.add(new ImportNote(ImportNoteKind.IGNORED, IcalTexts.titleOrUntitled(extra, locale), "DUPLICATE_UID"));
        });
        boolean repeats = master.getProperty(Property.RRULE).isPresent() || master.getProperty(Property.RDATE).isPresent();
        if (!repeats) {
            return single(uid, master, overrides, zone, locale, notes, ignored);
        }
        if (!usable(master, zone, locale, notes, ignored)) {
            return Optional.empty();
        }
        IcalTimes times = IcalTimes.of(master, zone).orElseThrow();
        int notesMark = notes.size();
        int ignoredMark = ignored[0];
        try {
            IcalTexts texts = IcalTexts.of(master, locale);
            notesOf(texts, times, notes);
            // An edited occurrence nido cannot hold is set aside and said so; a cancelled one is no event, only a slot to free.
            List<VEvent> held = overrides.stream()
                .filter(override -> cancelled(override) || usable(override, zone, locale, notes, ignored)).toList();
            RecurringImport.Result result = recurring.translate(master, held, texts, times, notes);
            if (result.events().isEmpty() && result.series() == null) {
                return Optional.empty();
            }
            if (result.unfolded()) {
                notes.add(new ImportNote(result.partial() ? ImportNoteKind.UNFOLDED_PARTIAL : ImportNoteKind.UNFOLDED,
                    texts.title(), String.valueOf(result.unfoldedOccurrences())));
            }
            return Optional.of(new ImportGroup(uid, result.events(), result.series(), result.unfolded(), result.unfoldedOccurrences()));
        } catch (BoundedExpansion.Refused refused) {
            return setAside(master, locale, notes, ignored, notesMark, ignoredMark, refused.detail());
        } catch (RuntimeException unreadable) {
            // The one broad catch of the reader: a third-party library expands rules from an untrusted file, and what it
            // throws on a rule it cannot handle (a date it cannot reach, a part it does not accept on a date) must cost
            // this event, not the whole file.
            return setAside(master, locale, notes, ignored, notesMark, ignoredMark, "BAD_RULE");
        }
    }

    /** A recurring event given up whole: what its reading had said is taken back, and only the reason is kept. */
    private static Optional<ImportGroup> setAside(VEvent master, Locale locale, List<ImportNote> notes, int[] ignored,
                                                  int notesMark, int ignoredMark, String reason) {
        notes.subList(notesMark, notes.size()).clear();
        ignored[0] = ignoredMark + 1;
        notes.add(new ImportNote(ImportNoteKind.IGNORED, IcalTexts.titleOrUntitled(master, locale), reason));
        return Optional.empty();
    }

    /**
     * An event that does not repeat. An override of its own start is a newer version of that one event, and replaces it
     * (cancelled, it removes it); any other override, standing for nothing the file holds, is an event of its own.
     */
    private static Optional<ImportGroup> single(String uid, VEvent master, List<VEvent> overrides, ZoneId zone, Locale locale,
                                                List<ImportNote> notes, int[] ignored) {
        List<ImportedEvent> events = new ArrayList<>();
        Optional<IcalTimes> own = IcalTimes.of(master, zone);
        Optional<VEvent> newer = own.isEmpty() ? Optional.empty() : overrides.stream()
            .filter(override -> RecurringImport.replaced(override, zone).filter(own.get().start()::equals).isPresent())
            .findFirst();
        boolean replaced = false;
        if (newer.isPresent()) {
            // Its newer version says what the event is now; if nido cannot hold that, the event stays as the master said.
            boolean cancelledNewer = cancelled(newer.get());
            boolean usableNewer = usable(newer.get(), zone, locale, notes, ignored);
            replaced = cancelledNewer || usableNewer;
            if (usableNewer) {
                standaloneUsable(newer.get(), zone, locale, notes).ifPresent(events::add);
            }
        }
        if (!replaced) {
            standalone(master, zone, locale, notes, ignored).ifPresent(events::add);
        }
        for (VEvent override : overrides) {
            if (newer.isEmpty() || override != newer.get()) {
                standalone(override, zone, locale, notes, ignored).ifPresent(events::add);
            }
        }
        return events.isEmpty() ? Optional.empty() : Optional.of(new ImportGroup(uid, events, null, false, 0));
    }

    /** An event taken on its own; set aside, counted and noted when it cannot be held. */
    private static Optional<ImportedEvent> standalone(VEvent vevent, ZoneId zone, Locale locale, List<ImportNote> notes,
                                                      int[] ignored) {
        if (!usable(vevent, zone, locale, notes, ignored)) {
            return Optional.empty();
        }
        return standaloneUsable(vevent, zone, locale, notes);
    }

    /** The event of one already found usable, with the notes its texts and times call for. */
    private static Optional<ImportedEvent> standaloneUsable(VEvent vevent, ZoneId zone, Locale locale, List<ImportNote> notes) {
        IcalTimes times = IcalTimes.of(vevent, zone).orElseThrow();
        IcalTexts texts = IcalTexts.of(vevent, locale);
        notesOf(texts, times, notes);
        return Optional.of(event(texts, times));
    }

    /**
     * Whether an event can be imported, decided before its texts are worked on: an event about to be set aside costs no
     * more than its title. When it cannot, says why in the report and counts it.
     */
    private static boolean usable(VEvent vevent, ZoneId zone, Locale locale, List<ImportNote> notes, int[] ignored) {
        String reason;
        if (cancelled(vevent)) {
            reason = "CANCELLED";
        } else {
            Optional<IcalTimes> times = IcalTimes.of(vevent, zone);
            reason = times.isEmpty() ? "NO_START" : fits(times.get()) ? null : "TOO_LONG";
        }
        if (reason == null) {
            return true;
        }
        ignored[0]++;
        notes.add(new ImportNote(ImportNoteKind.IGNORED, IcalTexts.titleOrUntitled(vevent, locale), reason));
        return false;
    }

    static void notesOf(IcalTexts texts, IcalTimes times, List<ImportNote> notes) {
        texts.cut().forEach(what -> notes.add(new ImportNote(ImportNoteKind.TRUNCATED_TEXT, texts.title(), what)));
        if (times.foreignZone() != null) {
            notes.add(new ImportNote(ImportNoteKind.CONVERTED_ZONE, texts.title(), times.foreignZone()));
        }
        if (times.unknownZone() != null) {
            notes.add(new ImportNote(ImportNoteKind.UNKNOWN_ZONE, texts.title(), times.unknownZone()));
        }
    }

    static ImportedEvent event(IcalTexts texts, IcalTimes times) {
        return new ImportedEvent(texts.title(), texts.description(), texts.location(), times.allDay(),
            times.startDate(), times.startTime(), times.endDate(), times.endTime());
    }

    static boolean cancelled(VEvent event) {
        return event.getProperty(Property.STATUS).map(Property::getValue).filter("CANCELLED"::equalsIgnoreCase).isPresent();
    }

    /** Whether nido can hold it as it is: no event covers more than a calendar read shows. */
    private static boolean fits(IcalTimes times) {
        try {
            EventScheduleValidator.validateEvent(times.allDay(), times.startDate(), times.startTime(), times.endDate(), times.endTime());
            return true;
        } catch (CalendarException refused) {
            return false;
        }
    }
}
