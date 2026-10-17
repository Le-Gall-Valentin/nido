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
        RecurringImport recurring = new RecurringImport(zone, today, locale);
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
        if (!usable(master, zone, locale, notes, ignored)) {
            return Optional.empty();
        }
        IcalTimes times = IcalTimes.of(master, zone).orElseThrow();
        IcalTexts texts = IcalTexts.of(master, locale);
        notesOf(texts, times, notes);
        boolean repeats = master.getProperty(Property.RRULE).isPresent() || master.getProperty(Property.RDATE).isPresent();
        if (!repeats) {
            return Optional.of(new ImportGroup(uid, List.of(event(texts, times)), null, false, 0));
        }
        RecurringImport.Result result = recurring.translate(master, overrides, texts, times);
        if (result.unfolded()) {
            notes.add(new ImportNote(result.partial() ? ImportNoteKind.UNFOLDED_PARTIAL : ImportNoteKind.UNFOLDED,
                texts.title(), String.valueOf(result.unfoldedOccurrences())));
        }
        return Optional.of(new ImportGroup(uid, result.events(), result.series(), result.unfolded(), result.unfoldedOccurrences()));
    }

    /** An event taken on its own; set aside, counted and noted when it cannot be held. */
    private static Optional<ImportedEvent> standalone(VEvent vevent, ZoneId zone, Locale locale, List<ImportNote> notes,
                                                      int[] ignored) {
        if (!usable(vevent, zone, locale, notes, ignored)) {
            return Optional.empty();
        }
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

    private static void notesOf(IcalTexts texts, IcalTimes times, List<ImportNote> notes) {
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
