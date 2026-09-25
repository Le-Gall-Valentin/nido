package com.nido.api.calendar.application.handler;

import com.nido.api.calendar.domain.model.CalendarException;
import com.nido.api.calendar.domain.model.CalendarOccurrence;
import com.nido.api.calendar.domain.model.CalendarSourceType;
import com.nido.api.calendar.domain.port.out.CalendarSource;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListCalendarOccurrencesHandlerTest {

    private final SpaceMembership caller = new SpaceMembership(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void mergesEverySourceAndOrdersByDateThenTime() {
        var handler = new ListCalendarOccurrencesHandler(List.of(
            stub(CalendarSourceType.MEAL, allDay("Gratin", LocalDate.of(2026, 1, 8))),
            stub(CalendarSourceType.EVENT,
                timed("Piano", LocalDate.of(2026, 1, 6), LocalTime.of(18, 0)),
                timed("Dentiste", LocalDate.of(2026, 1, 6), LocalTime.of(9, 0)))));

        assertThat(handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .extracting(CalendarOccurrence::title)
            .containsExactly("Dentiste", "Piano", "Gratin");
    }

    @Test
    void sortsAnAllDayEntryBeforeATimedOneOnTheSameDay() {
        var handler = new ListCalendarOccurrencesHandler(List.of(
            stub(CalendarSourceType.EVENT, timed("Piano", LocalDate.of(2026, 1, 6), LocalTime.of(0, 30))),
            stub(CalendarSourceType.TASK, allDay("Poubelles", LocalDate.of(2026, 1, 6)))));

        assertThat(handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .extracting(CalendarOccurrence::title)
            .containsExactly("Poubelles", "Piano");
    }

    @Test
    void refusesAWindowWiderThanTheMaximum() {
        var handler = new ListCalendarOccurrencesHandler(List.of());

        assertThatThrownBy(() -> handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 1)))
            .isInstanceOf(CalendarException.WindowTooLarge.class);
    }

    @Test
    void acceptsAWindowExactlyAtTheMaximum() {
        var handler = new ListCalendarOccurrencesHandler(List.of());

        assertThat(handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1).plusDays(365))).isEmpty();
    }

    @Test
    void refusesAWindowWhoseStartIsAfterItsEnd() {
        var handler = new ListCalendarOccurrencesHandler(List.of());

        assertThatThrownBy(() -> handler.list(caller, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
            .isInstanceOf(CalendarException.WindowTooLarge.class);
    }

    @Test
    void failsTheWholeReadWhenOneSourceFailsRatherThanShowingTheOthersAlone() {
        // A calendar that quietly drops your dentist appointment because one source broke is worse
        // than an error: the user cannot tell the difference between "nothing" and "not shown".
        CalendarSource broken = new CalendarSource() {
            @Override public CalendarSourceType type() { return CalendarSourceType.FINANCE; }
            @Override public List<CalendarOccurrence> occurrencesBetween(SpaceMembership c, LocalDate f, LocalDate t) {
                throw new IllegalStateException("boom");
            }
        };
        var handler = new ListCalendarOccurrencesHandler(List.of(broken));

        assertThatThrownBy(() -> handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnsNothingWhenNoSourceIsRegistered() {
        assertThat(new ListCalendarOccurrencesHandler(List.of())
            .list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).isEmpty();
    }

    private static CalendarSource stub(CalendarSourceType type, CalendarOccurrence... occurrences) {
        return new CalendarSource() {
            @Override public CalendarSourceType type() { return type; }
            @Override public List<CalendarOccurrence> occurrencesBetween(SpaceMembership c, LocalDate f, LocalDate t) {
                return List.of(occurrences);
            }
        };
    }

    private static CalendarOccurrence allDay(String title, LocalDate date) {
        return new CalendarOccurrence(CalendarSourceType.MEAL, UUID.randomUUID().toString(), null, null,
            true, title, null, null, true, date, null, date, null, null, List.of());
    }

    private static CalendarOccurrence timed(String title, LocalDate date, LocalTime time) {
        return new CalendarOccurrence(CalendarSourceType.EVENT, UUID.randomUUID().toString(), null, null,
            true, title, null, null, false, date, time, date, time.plusHours(1), null, List.of());
    }
}
