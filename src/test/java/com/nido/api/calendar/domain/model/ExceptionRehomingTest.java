package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionRehomingTest {

    /** Piano moved from Mondays to Tuesdays: the new schedule's occurrences fall on 6, 13, 20, 27 October. */
    private final RecurringEventSeries tuesdays = new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano",
        null, null, true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 10, 6), null,
        List.of(), UUID.randomUUID(), Instant.now());

    @Test
    void anEditedOccurrenceAndACancellationEachTakeTheNearestOccurrenceOfTheNewSchedule() {
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(tuesdays,
            List.of(LocalDate.of(2026, 10, 12)), List.of(LocalDate.of(2026, 10, 19)));

        assertThat(plan.editedOccurrences()).containsExactly(Map.entry(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)));
        assertThat(plan.cancellations()).containsExactly(LocalDate.of(2026, 10, 20));
    }

    @Test
    void twoEditedOccurrencesNearestToTheSameOneLeaveItToTheCloser() {
        // Sunday 11 and Monday 12 both lie nearest to Tuesday 13: the Monday, a day away, takes it.
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(tuesdays,
            List.of(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12)), List.of());

        assertThat(plan.editedOccurrences()).containsExactly(Map.entry(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13)));
    }

    @Test
    void aCancellationNeverTakesTheOccurrenceAnEditedOneTook() {
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(tuesdays,
            List.of(LocalDate.of(2026, 10, 12)), List.of(LocalDate.of(2026, 10, 14)));

        assertThat(plan.editedOccurrences()).containsEntry(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 13));
        assertThat(plan.cancellations()).isEmpty();
    }

    @Test
    void anExceptionFarFromEveryOccurrenceHasNoPlace() {
        RecurringEventSeries endedEarly = new RecurringEventSeries(tuesdays.id(), tuesdays.spaceId(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 13),
            List.of(), UUID.randomUUID(), Instant.now());

        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(endedEarly,
            List.of(LocalDate.of(2026, 11, 2)), List.of(LocalDate.of(2026, 11, 9)));

        assertThat(plan.editedOccurrences()).isEmpty();
        assertThat(plan.cancellations()).isEmpty();
    }
}
