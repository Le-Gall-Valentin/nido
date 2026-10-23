package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
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

    @Test
    void showsNothingBeforeTheDayASeriesCarriedOnFrom() {
        RecurringEventSeries series = carriedOn(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .containsExactly(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 27));
    }

    @Test
    void keepsTheRhythmOfAMonthlySeriesCarriedOnInFebruary() {
        // The 31st of each month, carried on from February 10: February's is the 28th, March's the 31st again.
        RecurringEventSeries series = carriedOn(RecurrenceInterval.MONTHLY, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 2, 10));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
            .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
    }

    @Test
    void firstOccurrenceIsTheFirstOneShown() {
        assertThat(EventRecurrenceProjector.firstOccurrence(weekly(LocalDate.of(2026, 1, 6), null, 0)))
            .contains(LocalDate.of(2026, 1, 6));
        assertThat(EventRecurrenceProjector.firstOccurrence(
            carriedOn(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 15))))
            .contains(LocalDate.of(2026, 1, 20));
    }

    @Test
    void aSeriesEndingBeforeItsFirstOccurrenceHasNone() {
        RecurringEventSeries ended = new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 10),
            LocalDate.of(2026, 1, 8), List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.firstOccurrence(ended)).isEmpty();
    }

    @Test
    void theNearestSlotOfATuesdaySeriesToAMondayIsTheNextDay() {
        RecurringEventSeries tuesdays = weekly(LocalDate.of(2026, 10, 6), null, 0);
        assertThat(EventRecurrenceProjector.nearestSlot(tuesdays, LocalDate.of(2026, 10, 12)))
            .contains(LocalDate.of(2026, 10, 13));
    }

    @Test
    void twoSlotsAtTheSameDistanceGiveTheLaterOne() {
        RecurringEventSeries everyOtherDay = new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.DAILY, 2, LocalDate.of(2026, 10, 1), null,
            List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.nearestSlot(everyOtherDay, LocalDate.of(2026, 10, 4)))
            .contains(LocalDate.of(2026, 10, 5));
    }

    @Test
    void theNearestSlotIsNeverOneTheSeriesDoesNotShow() {
        RecurringEventSeries fromThe20th = carriedOn(RecurrenceInterval.WEEKLY, LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 20));
        // Jan 13 is a slot of the rhythm, but before the series shows anything: Jan 20 is the one.
        assertThat(EventRecurrenceProjector.nearestSlot(fromThe20th, LocalDate.of(2026, 1, 14)))
            .contains(LocalDate.of(2026, 1, 20));
        RecurringEventSeries ended = weekly(LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 13), 0);
        assertThat(EventRecurrenceProjector.nearestSlot(ended, LocalDate.of(2026, 3, 2))).isEmpty();
    }

    @Test
    void aWeeklySeriesOnSeveralDaysProducesEachOfThem() {
        // 2026-01-05 is a Monday.
        RecurringEventSeries series = ruled(RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            null, LocalDate.of(2026, 1, 5));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 18)))
            .containsExactly(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 15));
    }

    @Test
    void aWeekOfTheAnchorShowsNoDayBeforeTheAnchor() {
        // Anchored on Thursday the 8th: the Monday of that week is not an occurrence.
        RecurringEventSeries series = ruled(RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
            null, LocalDate.of(2026, 1, 8));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 14)))
            .containsExactly(LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 12));
    }

    @Test
    void everyOtherWeekCountsWeeksFromTheAnchorsAcrossTheNewYear() {
        // Monday 28 Dec 2026, every two weeks on Monday and Friday: weeks of 28 Dec, 11 Jan, 25 Jan.
        RecurringEventSeries series = ruled(RecurrenceInterval.WEEKLY, 2, Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            null, LocalDate.of(2026, 12, 28));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 12, 1), LocalDate.of(2027, 1, 31)))
            .containsExactly(LocalDate.of(2026, 12, 28), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 11),
                LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 25), LocalDate.of(2027, 1, 29));
    }

    @Test
    void aMonthlySeriesOnTheSecondTuesday() {
        RecurringEventSeries series = ruled(RecurrenceInterval.MONTHLY, 1, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 13));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31)))
            .containsExactly(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 12, 8), LocalDate.of(2027, 1, 12));
    }

    @Test
    void aMonthlySeriesOnTheLastFridayFindsItInFourAndFiveWeekMonths() {
        RecurringEventSeries series = ruled(RecurrenceInterval.MONTHLY, 1, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 30));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 1, 31)))
            .containsExactly(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 27), LocalDate.of(2026, 12, 25), LocalDate.of(2027, 1, 29));
    }

    @Test
    void aMonthlySeriesOnTheLastDayFollowsFebruary() {
        RecurringEventSeries series = ruled(RecurrenceInterval.MONTHLY, 1, null, DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2027, 11, 30));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2027, 11, 1), LocalDate.of(2028, 3, 31)))
            .containsExactly(LocalDate.of(2027, 11, 30), LocalDate.of(2027, 12, 31), LocalDate.of(2028, 1, 31),
                LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 31));
    }

    @Test
    void aYearlySeriesOnTheFourthThursdayOfNovember() {
        RecurringEventSeries series = ruled(RecurrenceInterval.YEARLY, 1, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 11, 26));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2028, 12, 31)))
            .containsExactly(LocalDate.of(2026, 11, 26), LocalDate.of(2027, 11, 25), LocalDate.of(2028, 11, 23));
    }

    @Test
    void aYearlySeriesOnTheLastSundayOfMay() {
        RecurringEventSeries series = ruled(RecurrenceInterval.YEARLY, 1, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 5, 31));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 12, 31)))
            .containsExactly(LocalDate.of(2026, 5, 31), LocalDate.of(2027, 5, 30));
    }

    @Test
    void aYearlySeriesOnFebruaryThe29thFallsOnThe28thInOtherYears() {
        RecurringEventSeries series = ruled(RecurrenceInterval.YEARLY, 1, null, null, LocalDate.of(2028, 2, 29));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2028, 1, 1), LocalDate.of(2030, 12, 31)))
            .containsExactly(LocalDate.of(2028, 2, 29), LocalDate.of(2029, 2, 28), LocalDate.of(2030, 2, 28));
    }

    @Test
    void aSeriesCarriedOnFromTheMiddleOfAWeekKeepsItsWeekdays() {
        RecurringEventSeries series = new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
            LocalDate.of(2026, 1, 5), null, LocalDate.of(2026, 1, 14), List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.firstOccurrence(series)).contains(LocalDate.of(2026, 1, 15));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 20)))
            .containsExactly(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 19));
    }

    @Test
    void aFarAnchorCostsNoMoreThanItsWindow() {
        RecurringEventSeries series = ruled(RecurrenceInterval.WEEKLY, 1, Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            null, LocalDate.of(1900, 1, 1));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18)))
            .containsExactly(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
    }

    @Test
    void aMonthlySeriesOnTheThirtiethFallsOnTheLastDayOfFebruary() {
        RecurringEventSeries series = ruled(RecurrenceInterval.MONTHLY, 1, null, DayRule.SAME_DAY, LocalDate.of(2027, 1, 30));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 4, 30)))
            .containsExactly(LocalDate.of(2027, 1, 30), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 30), LocalDate.of(2027, 4, 30));
    }

    @Test
    void everyOtherMonthOnTheSecondTuesdayCountsMonthsFromTheAnchor() {
        RecurringEventSeries series = ruled(RecurrenceInterval.MONTHLY, 2, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(2026, 10, 13));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 10, 1), LocalDate.of(2027, 4, 30)))
            .containsExactly(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 12, 8), LocalDate.of(2027, 2, 9), LocalDate.of(2027, 4, 13));
    }

    @Test
    void aDayRuleAnchoredDecadesAgoFindsItsDayInTheWindow() {
        RecurringEventSeries lastFriday = ruled(RecurrenceInterval.MONTHLY, 1, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(1990, 1, 26));
        assertThat(EventRecurrenceProjector.slotsBetween(lastFriday, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 30)))
            .containsExactly(LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 27));
        RecurringEventSeries thanksgiving = ruled(RecurrenceInterval.YEARLY, 1, null, DayRule.WEEKDAY_OF_MONTH, LocalDate.of(1950, 11, 23));
        assertThat(EventRecurrenceProjector.slotsBetween(thanksgiving, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
            .containsExactly(LocalDate.of(2026, 11, 26));
    }

    @Test
    void theDaysOfAWeekComeInTheirOrderWhateverTheOrderGiven() {
        // Sunday before Monday as given: a week runs from Monday, so the Monday of a period comes first.
        RecurringEventSeries series = ruled(RecurrenceInterval.WEEKLY, 1,
            new LinkedHashSet<>(List.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)), null, LocalDate.of(2026, 10, 12));
        assertThat(EventRecurrenceProjector.datesOfPeriod(series, 0))
            .containsExactly(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18));
        assertThat(EventRecurrenceProjector.slotsBetween(series, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 26)))
            .isSorted().containsExactly(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 26));
    }

    @Test
    void aSeriesStoredWithAnIntervalPastTheLastDateACalendarHoldsShowsWhatComesBefore() {
        // Saved before the interval was bounded: its next year, a billion years on, is no date at all.
        RecurringEventSeries everyBillionYears = ruled(RecurrenceInterval.YEARLY, 1_000_000_000, null, null,
            LocalDate.of(2026, 1, 6));
        assertThat(EventRecurrenceProjector.slotsBetween(everyBillionYears, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 31)))
            .containsExactly(LocalDate.of(2026, 1, 6));
        assertThat(EventRecurrenceProjector.slotsBetween(everyBillionYears, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)))
            .isEmpty();

        RecurringEventSeries carriedOn = new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, RecurrenceInterval.YEARLY, Integer.MAX_VALUE, null, null, LocalDate.of(2026, 1, 6),
            null, LocalDate.of(2026, 6, 1), List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.firstOccurrence(carriedOn)).isEmpty();
        assertThat(EventRecurrenceProjector.slotsBetween(carriedOn, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))).isEmpty();
        assertThat(EventRecurrenceProjector.runsAcross(carriedOn, LocalDate.of(2026, 7, 1))).isFalse();
    }

    @Test
    void aSeriesIsNeverLookedForBeforeTheDayItIsShownFrom() {
        // A duration stored before occurrences were bounded reached back past the first date the database holds.
        RecurringEventSeries longAgo = new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 3_650_000, null, RecurrenceInterval.YEARLY, 10_000, LocalDate.of(2026, 1, 6), null,
            List.of(), UUID.randomUUID(), Instant.now());
        assertThat(EventRecurrenceProjector.scanFrom(longAgo, LocalDate.of(2026, 10, 1))).isEqualTo(LocalDate.of(2026, 1, 6));
        assertThat(EventRecurrenceProjector.scanFrom(weekly(LocalDate.of(2026, 1, 6), null, 3), LocalDate.of(2026, 10, 1)))
            .isEqualTo(LocalDate.of(2026, 9, 28));
    }

    private RecurringEventSeries ruled(RecurrenceInterval interval, int count, Set<DayOfWeek> weekdays, DayRule dayRule,
                                       LocalDate anchor) {
        return new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, interval, count, weekdays, dayRule, anchor, null, null,
            List.of(), UUID.randomUUID(), Instant.now());
    }

    private RecurringEventSeries carriedOn(RecurrenceInterval interval, LocalDate anchor, LocalDate startsOn) {
        return new RecurringEventSeries(SERIES_ID, UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, interval, 1, anchor, null, startsOn, List.of(), UUID.randomUUID(), Instant.now());
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
