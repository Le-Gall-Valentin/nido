package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Test
    void anEditedOccurrenceFindsItsNearestDayAmongSeveralWeekdays() {
        // Monday and Thursday from 5 Jan 2026: an occurrence edited on Wednesday the 7th goes to Thursday the 8th.
        RecurringEventSeries target = new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
            LocalDate.of(2026, 1, 5), null, null, List.of(), UUID.randomUUID(), Instant.now());
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(target, List.of(LocalDate.of(2026, 1, 7)), List.of());
        assertThat(plan.editedOccurrences()).containsEntry(LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 8));
    }

    @Test
    void aWednesdaySeriesMovedToMondayAndTuesdayKeepsWhatWasChosenOnItsOwn() {
        // Wednesday 21 October cancelled, Wednesday 28 edited: two days apart at the closest, but six from Tuesday to Monday.
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(weekly(1, LocalDate.of(2026, 10, 5), DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
            List.of(LocalDate.of(2026, 10, 28)), List.of(LocalDate.of(2026, 10, 21)));

        assertThat(plan.editedOccurrences()).containsExactly(Map.entry(LocalDate.of(2026, 10, 28), LocalDate.of(2026, 10, 27)));
        assertThat(plan.cancellations()).containsExactly(LocalDate.of(2026, 10, 20));
    }

    @Test
    void anExceptionFindsItsPlaceAcrossTheWidestGapOfASeriesOnSeveralWeekdays() {
        // Monday and Thursday every other week from 5 Oct: 5, 8, 19, 22 Oct. Monday 12 and Thursday 15 lie in a week skipped.
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(weekly(2, LocalDate.of(2026, 10, 5), DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            List.of(LocalDate.of(2026, 10, 12)), List.of(LocalDate.of(2026, 10, 15)));

        assertThat(plan.editedOccurrences()).containsExactly(Map.entry(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 8)));
        assertThat(plan.cancellations()).containsExactly(LocalDate.of(2026, 10, 19));
    }

    @Test
    void aSeriesMadeEveryOtherWeekFromItsStartShownKeepsAnExceptionOfAWeekItSkipsOnTheNearestOccurrence() {
        // The form anchors a series whose interval changes on its start shown, 7 October: now 7 and 21 October, 4 and
        // 18 November. Edited on the 21st, still an occurrence, it stays; edited on 11 November or cancelled on 28 October,
        // weeks now skipped, each takes the later of the two occurrences a week away.
        ExceptionRehoming.Plan plan = ExceptionRehoming.plan(weekly(2, LocalDate.of(2026, 10, 7), DayOfWeek.WEDNESDAY),
            List.of(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 11)), List.of(LocalDate.of(2026, 10, 28)));

        assertThat(plan.editedOccurrences()).containsOnly(Map.entry(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 21)),
            Map.entry(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 18)));
        assertThat(plan.cancellations()).containsExactly(LocalDate.of(2026, 11, 4));
    }

    private static RecurringEventSeries weekly(int interval, LocalDate anchor, DayOfWeek... days) {
        return new RecurringEventSeries(UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null, true, null, null, 0, null,
            RecurrenceInterval.WEEKLY, interval, Set.of(days), null, anchor, null, null, List.of(), UUID.randomUUID(),
            Instant.now());
    }
}
