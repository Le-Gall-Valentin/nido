package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventRecurrenceProjectorTest {

    private static final UUID SERIES_ID = UUID.randomUUID();

    @Test
    void producesEveryWeeklySlotInsideTheWindow() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), null, 0);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .containsExactly(
                LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 13),
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 27));
    }

    @Test
    void neverProducesASlotBeforeTheAnchor() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 20), null, 0);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .containsExactly(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 27));
    }

    @Test
    void stopsAtTheSeriesEndDate() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 15), 0);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 13));
    }

    @Test
    void clampsAMonthlyAnchorToShorterMonthsWithoutDrifting() {
        RecurringEventSeries series = monthly(LocalDate.of(2026, 1, 31));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)))
            .containsExactly(
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
    }

    @Test
    void includesAMultiDayOccurrenceThatStartedBeforeTheWindow() {
        // Starts Jan 5 and runs three days, so it is still running on Jan 6 — the window's first day.
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 5), null, 3);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7)))
            .containsExactly(LocalDate.of(2026, 1, 5));
    }

    @Test
    void doesNotIncludeAMultiDayOccurrenceThatFinishedBeforeTheWindow() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 1), null, 2);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6)))
            .isEmpty();
    }

    @Test
    void dropsAnExcludedSlot() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), null, 0);
        List<CalendarOccurrence> produced = EventRecurrenceProjector.project(
            series, Set.of(LocalDate.of(2026, 1, 13)), Set.of(),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20));
        assertThat(produced).extracting(CalendarOccurrence::originalDate)
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
    }

    @Test
    void dropsASlotTakenOverByADetachedInstance() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), null, 0);
        List<CalendarOccurrence> produced = EventRecurrenceProjector.project(
            series, Set.of(), Set.of(LocalDate.of(2026, 1, 13)),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20));
        assertThat(produced).extracting(CalendarOccurrence::originalDate)
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
    }

    @Test
    void marksProjectedOccurrencesAsNotMaterializedAndKeysThemBySlot() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), null, 0);
        CalendarOccurrence first = EventRecurrenceProjector.project(
            series, Set.of(), Set.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7)).getFirst();

        assertThat(first.materialized()).isFalse();
        assertThat(first.source()).isEqualTo(CalendarSourceType.EVENT);
        assertThat(first.sourceId()).isEqualTo(SERIES_ID + ":2026-01-06");
        assertThat(first.seriesId()).isEqualTo(SERIES_ID);
        assertThat(first.endDate()).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(first.startTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void carriesTheSeriesDurationIntoEachOccurrencesEndDate() {
        RecurringEventSeries series = weekly(LocalDate.of(2026, 1, 6), null, 2);
        CalendarOccurrence first = EventRecurrenceProjector.project(
            series, Set.of(), Set.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7)).getFirst();
        assertThat(first.endDate()).isEqualTo(LocalDate.of(2026, 1, 8));
    }

    @Test
    void handlesAnEveryThreeWeeksInterval() {
        RecurringEventSeries series = new RecurringEventSeries(
            SERIES_ID, UUID.randomUUID(), "Piano", null, null, true, null, null, 0, null,
            RecurrenceInterval.WEEKLY, 3, LocalDate.of(2026, 1, 6), null, List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)))
            .containsExactly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 27), LocalDate.of(2026, 2, 17));
    }

    @Test
    void findsTheRightStartingSlotForAnAnchorDecadesInThePast() {
        // Guards the arithmetic shortcut: the cost must depend on the window, not on the anchor's age.
        // 1990-01-01 is a Monday, so every slot is a Monday — 2026-01-05 and 2026-01-12 here.
        RecurringEventSeries series = weekly(LocalDate.of(1990, 1, 1), null, 0);
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12)))
            .containsExactly(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12));
    }

    @Test
    void carriesTheSeriesDescriptionAndLocationIntoEachOccurrence() {
        // Editing an occurrence starts from what the feed says it is. Without these two fields the
        // form opened empty and saving wiped them — a silent loss the reader never asked for.
        RecurringEventSeries series = new RecurringEventSeries(
            SERIES_ID, UUID.randomUUID(), "Piano", "Apporter la partition", "Conservatoire", false,
            LocalTime.of(18, 0), LocalTime.of(19, 0), 0, null,
            RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 6), null, List.of(), UUID.randomUUID(), Instant.now());

        CalendarOccurrence first = EventRecurrenceProjector.project(
            series, Set.of(), Set.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 7)).getFirst();

        assertThat(first.description()).isEqualTo("Apporter la partition");
        assertThat(first.location()).isEqualTo("Conservatoire");
    }

    private RecurringEventSeries weekly(LocalDate anchor, LocalDate end, int durationDays) {
        return series(RecurrenceInterval.WEEKLY, anchor, end, durationDays);
    }

    private RecurringEventSeries monthly(LocalDate anchor) {
        return series(RecurrenceInterval.MONTHLY, anchor, null, 0);
    }

    private RecurringEventSeries series(RecurrenceInterval interval, LocalDate anchor, LocalDate end, int durationDays) {
        return new RecurringEventSeries(
            SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            false, LocalTime.of(18, 0), LocalTime.of(19, 0), durationDays, null,
            interval, 1, anchor, end, List.of(), UUID.randomUUID(), Instant.now());
    }
}
