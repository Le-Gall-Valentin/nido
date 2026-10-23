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

class ListAgendaEntriesHandlerTest {

    private final SpaceMembership caller = new SpaceMembership(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

    @Test
    void returns_only_the_events_ordered_all_day_first_then_by_time() {
        var handler = new ListAgendaEntriesHandler(List.of(
            source(CalendarSourceType.EVENT,
                timed("Dîner chez Paul", LocalDate.of(2026, 9, 26), LocalTime.of(19, 30)),
                timed("Marché", LocalDate.of(2026, 9, 26), LocalTime.of(10, 30)),
                allDay("Anniversaire de Léa", LocalDate.of(2026, 9, 26))),
            source(CalendarSourceType.MEAL, allDay("Curry", LocalDate.of(2026, 9, 26)))));

        assertThat(handler.list(caller, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 27)))
            .extracting(CalendarOccurrence::title)
            .containsExactly("Anniversaire de Léa", "Marché", "Dîner chez Paul");
    }

    @Test
    void never_runs_a_source_other_than_the_events_one() {
        // A task or finance source that fails must not be able to break a read that never needed it.
        CalendarSource failingTasks = new CalendarSource() {
            @Override public CalendarSourceType type() { return CalendarSourceType.TASK; }
            @Override public List<CalendarOccurrence> occurrencesBetween(SpaceMembership c, LocalDate f, LocalDate t) {
                throw new IllegalStateException("the tasks source must not run");
            }
        };
        var handler = new ListAgendaEntriesHandler(List.of(failingTasks,
            source(CalendarSourceType.EVENT, allDay("Anniversaire de Léa", LocalDate.of(2026, 9, 26)))));

        assertThat(handler.list(caller, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 26))).hasSize(1);
    }

    @Test
    void reads_the_calendars_a_space_follows_besides_its_events() {
        var handler = new ListAgendaEntriesHandler(List.of(
            source(CalendarSourceType.EVENT, allDay("Anniversaire de Léa", LocalDate.of(2026, 9, 26))),
            source(CalendarSourceType.EXTERNAL, allDay("Vacances", LocalDate.of(2026, 9, 26))),
            source(CalendarSourceType.TASK, allDay("Linge", LocalDate.of(2026, 9, 26)))));

        assertThat(handler.list(caller, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 26)))
            .extracting(CalendarOccurrence::title).containsExactlyInAnyOrder("Anniversaire de Léa", "Vacances");
    }

    @Test
    void the_followed_calendars_give_the_agenda_at_most_two_hundred_entries() {
        CalendarOccurrence[] many = new CalendarOccurrence[500];
        for (int i = 0; i < many.length; i++) {
            LocalDate day = LocalDate.of(2026, 9, 26);
            many[i] = new CalendarOccurrence(CalendarSourceType.EXTERNAL, "e" + i, null, null, true, "Externe " + i, null,
                null, true, day, null, day, null, null, List.of());
        }
        var handler = new ListAgendaEntriesHandler(List.of(
            source(CalendarSourceType.EXTERNAL, many),
            source(CalendarSourceType.EVENT, allDay("Anniversaire de Léa", LocalDate.of(2026, 9, 26)))));

        List<CalendarOccurrence> listed = handler.list(caller, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 9, 26));

        assertThat(listed).filteredOn(o -> o.source() == CalendarSourceType.EXTERNAL).hasSize(200);
        assertThat(listed).extracting(CalendarOccurrence::title).contains("Anniversaire de Léa");
    }

    @Test
    void refuses_a_window_whose_start_is_after_its_end() {
        var handler = new ListAgendaEntriesHandler(List.of());

        assertThatThrownBy(() -> handler.list(caller, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
            .isInstanceOf(CalendarException.ReversedWindow.class);
    }

    @Test
    void refuses_a_window_wider_than_the_maximum() {
        var handler = new ListAgendaEntriesHandler(List.of());

        assertThatThrownBy(() -> handler.list(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 1)))
            .isInstanceOf(CalendarException.WindowTooLarge.class);
    }

    private static CalendarSource source(CalendarSourceType type, CalendarOccurrence... occurrences) {
        return new CalendarSource() {
            @Override public CalendarSourceType type() { return type; }
            @Override public List<CalendarOccurrence> occurrencesBetween(SpaceMembership c, LocalDate f, LocalDate t) {
                return List.of(occurrences);
            }
        };
    }

    private static CalendarOccurrence allDay(String title, LocalDate date) {
        return new CalendarOccurrence(CalendarSourceType.EVENT, UUID.randomUUID().toString(), null, null,
            true, title, null, null, true, date, null, date, null, null, List.of());
    }

    private static CalendarOccurrence timed(String title, LocalDate date, LocalTime time) {
        return new CalendarOccurrence(CalendarSourceType.EVENT, UUID.randomUUID().toString(), null, null,
            true, title, null, null, false, date, time, date, time.plusHours(1), null, List.of());
    }
}
