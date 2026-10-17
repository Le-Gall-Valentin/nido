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
        int ignored = 0;
        for (String name : SET_ASIDE) {
            for (CalendarComponent other : calendar.<CalendarComponent>getComponents(name)) {
                ignored++;
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
            Optional<ImportGroup> group = group(uid, entry.getValue(), zone, locale, recurring, notes);
            if (group.isPresent()) {
                groups.add(group.get());
            } else {
                ignored++;
            }
        }
        return new ImportReading(groups, ignored, notes);
    }

    private static Optional<ImportGroup> group(String uid, List<VEvent> events, ZoneId zone, Locale locale,
                                               RecurringImport recurring, List<ImportNote> notes) {
        VEvent master = events.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isEmpty()).findFirst().orElse(null);
        List<VEvent> overrides = events.stream().filter(e -> e.getProperty(Property.RECURRENCE_ID).isPresent()).toList();
        if (master == null) {
            // Edited occurrences of a series the file does not hold: each is an event of its own.
            List<ImportedEvent> orphans = overrides.stream().filter(e -> !cancelled(e))
                .flatMap(e -> IcalTimes.of(e, zone).map(t -> event(IcalTexts.of(e, locale), t)).stream())
                .filter(Ical4jImportReaderAdapter::fits).toList();
            return orphans.isEmpty() ? Optional.empty() : Optional.of(new ImportGroup(uid, orphans, null, false, 0));
        }
        IcalTexts texts = IcalTexts.of(master, locale);
        if (cancelled(master)) {
            notes.add(new ImportNote(ImportNoteKind.IGNORED, texts.title(), "CANCELLED"));
            return Optional.empty();
        }
        Optional<IcalTimes> times = IcalTimes.of(master, zone);
        if (times.isEmpty()) {
            notes.add(new ImportNote(ImportNoteKind.IGNORED, texts.title(), "NO_START"));
            return Optional.empty();
        }
        if (!fits(event(texts, times.get()))) {
            notes.add(new ImportNote(ImportNoteKind.IGNORED, texts.title(), "TOO_LONG"));
            return Optional.empty();
        }
        texts.cut().forEach(what -> notes.add(new ImportNote(ImportNoteKind.TRUNCATED_TEXT, texts.title(), what)));
        if (times.get().foreignZone() != null) {
            notes.add(new ImportNote(ImportNoteKind.CONVERTED_ZONE, texts.title(), times.get().foreignZone()));
        }
        if (times.get().unknownZone() != null) {
            notes.add(new ImportNote(ImportNoteKind.UNKNOWN_ZONE, texts.title(), times.get().unknownZone()));
        }
        boolean repeats = master.getProperty(Property.RRULE).isPresent() || master.getProperty(Property.RDATE).isPresent();
        if (!repeats) {
            return Optional.of(new ImportGroup(uid, List.of(event(texts, times.get())), null, false, 0));
        }
        RecurringImport.Result result = recurring.translate(master, overrides, texts, times.get());
        if (result.unfolded()) {
            notes.add(new ImportNote(result.partial() ? ImportNoteKind.UNFOLDED_PARTIAL : ImportNoteKind.UNFOLDED,
                texts.title(), String.valueOf(result.unfoldedOccurrences())));
        }
        return Optional.of(new ImportGroup(uid, result.events(), result.series(), result.unfolded(), result.unfoldedOccurrences()));
    }

    static ImportedEvent event(IcalTexts texts, IcalTimes times) {
        return new ImportedEvent(texts.title(), texts.description(), texts.location(), times.allDay(),
            times.startDate(), times.startTime(), times.endDate(), times.endTime());
    }

    static boolean cancelled(VEvent event) {
        return event.getProperty(Property.STATUS).map(Property::getValue).filter("CANCELLED"::equalsIgnoreCase).isPresent();
    }

    /** Whether nido can hold it as it is: no event covers more than a calendar read shows. */
    private static boolean fits(ImportedEvent event) {
        try {
            EventScheduleValidator.validateEvent(event.allDay(), event.startDate(), event.startTime(), event.endDate(), event.endTime());
            return true;
        } catch (CalendarException refused) {
            return false;
        }
    }
}
