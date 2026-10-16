package com.nido.api.calendar.infrastructure.ical;

import com.nido.api.calendar.domain.model.CalendarEvent;
import com.nido.api.calendar.domain.model.CalendarFeed;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.model.FeedSeries;
import com.nido.api.calendar.domain.model.RecurrenceInterval;
import com.nido.api.calendar.domain.model.RecurringEventSeries;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Ical4jCalendarWriterAdapterTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private final Ical4jCalendarWriterAdapter writer = new Ical4jCalendarWriterAdapter();

    @Test
    void theCalendarIsNamedAfterTheSpaceAndKeepsItsZone() {
        String ics = writer.write(new CalendarFeed("Chez nous", PARIS, List.of(), List.of(), List.of()), Locale.FRENCH);
        assertThat(ics).contains("X-WR-CALNAME:Chez nous", "X-WR-TIMEZONE:Europe/Paris", "METHOD:PUBLISH",
            "REFRESH-INTERVAL;VALUE=DURATION:PT1H", "BEGIN:VTIMEZONE", "TZID:Europe/Paris");
    }

    @Test
    void anAllDayEventEndsTheDayAfterItsLastDay() {
        CalendarEvent holiday = event(true, LocalDate.of(2026, 2, 10), null, LocalDate.of(2026, 2, 12), null);
        String ics = writer.write(feed(List.of(holiday), List.of(), List.of()), Locale.FRENCH);
        assertThat(ics).contains("UID:" + holiday.id() + "@nido", "DTSTART;VALUE=DATE:20260210", "DTEND;VALUE=DATE:20260213");
    }

    @Test
    void aTimedEventIsWrittenInTheSpacesZone() {
        CalendarEvent concert = event(false, LocalDate.of(2026, 9, 23), LocalTime.of(20, 0), LocalDate.of(2026, 9, 23), LocalTime.of(22, 30));
        String ics = writer.write(feed(List.of(concert), List.of(), List.of()), Locale.FRENCH);
        assertThat(ics).contains("DTSTART;TZID=Europe/Paris:20260923T200000", "DTEND;TZID=Europe/Paris:20260923T223000",
            "LOCATION:Salle Pleyel", "COLOR:darkcyan");
    }

    @Test
    void aSeriesCarriesItsRuleItsCancellationsAndItsEditedOccurrences() {
        RecurringEventSeries piano = new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            false, LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null, RecurrenceInterval.WEEKLY, 1,
            Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null, LocalDate.of(2026, 1, 5), null, null,
            List.of(), UUID.randomUUID(), Instant.now());
        CalendarEvent moved = new CalendarEvent(UUID.randomUUID(), piano.spaceId(), "Piano (salle 3)", null, null, false,
            LocalDate.of(2026, 1, 13), LocalTime.of(18, 0), LocalDate.of(2026, 1, 13), LocalTime.of(19, 0), null, List.of(),
            piano.id(), LocalDate.of(2026, 1, 12), UUID.randomUUID(), Instant.now());
        String ics = writer.write(feed(List.of(), List.of(new FeedSeries(piano, Set.of(LocalDate.of(2026, 1, 8)), List.of(moved))),
            List.of()), Locale.FRENCH);

        assertThat(ics).contains("RRULE:FREQ=WEEKLY;WKST=MO;BYDAY=MO,TH", "EXDATE;TZID=Europe/Paris:20260108T180000",
            "RECURRENCE-ID;TZID=Europe/Paris:20260112T180000", "SUMMARY:Piano (salle 3)");
        assertThat(IcalTestSupport.starts(IcalTestSupport.master(ics, piano.id() + "@nido"), PARIS,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 15)))
            .containsExactly(LocalDateTime.of(2026, 1, 5, 18, 0), LocalDateTime.of(2026, 1, 12, 18, 0), LocalDateTime.of(2026, 1, 15, 18, 0));
    }

    @Test
    void theOtherSourcesSayWhatTheyAreInTheReadersLanguage() {
        CalendarOccurrence task = new CalendarOccurrence(CalendarSourceType.TASK, "t1", null, null, true,
            "Sortir les poubelles", null, null, true, LocalDate.of(2026, 10, 14), null, LocalDate.of(2026, 10, 14), null,
            null, List.of());
        CalendarOccurrence rent = new CalendarOccurrence(CalendarSourceType.FINANCE, "s1:2026-11-01", UUID.randomUUID(),
            LocalDate.of(2026, 11, 1), false, "Loyer", null, null, true, LocalDate.of(2026, 11, 1), null,
            LocalDate.of(2026, 11, 1), null, null, List.of());

        assertThat(writer.write(feed(List.of(), List.of(), List.of(task, rent)), Locale.FRENCH))
            .contains("SUMMARY:Tâche · Sortir les poubelles", "SUMMARY:Finance · Loyer", "UID:TASK-t1@nido",
                "UID:FINANCE-s1:2026-11-01@nido");
        assertThat(writer.write(feed(List.of(), List.of(), List.of(task)), Locale.ENGLISH))
            .contains("SUMMARY:Task · Sortir les poubelles");
    }

    private static CalendarFeed feed(List<CalendarEvent> events, List<FeedSeries> series, List<CalendarOccurrence> others) {
        return new CalendarFeed("Chez nous", PARIS, events, series, others);
    }

    private static CalendarEvent event(boolean allDay, LocalDate start, LocalTime startTime, LocalDate end, LocalTime endTime) {
        return new CalendarEvent(UUID.randomUUID(), UUID.randomUUID(), "Concert", "Apporter les billets", "Salle Pleyel",
            allDay, start, startTime, end, endTime, "event-cyan", List.of(), null, null, UUID.randomUUID(), Instant.now());
    }
}
