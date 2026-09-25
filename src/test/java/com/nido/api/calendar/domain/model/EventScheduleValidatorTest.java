package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventScheduleValidatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 1, 1);

    @Test
    void anEventMayCoverAYearOfDays() {
        assertThatCode(() -> EventScheduleValidator.validateEvent(true, DAY, null, DAY.plusDays(365), null))
            .doesNotThrowAnyException();
    }

    @Test
    void anEventMayNotCoverMoreDaysThanACalendarReadShows() {
        assertThatThrownBy(() -> EventScheduleValidator.validateEvent(true, DAY, null, DAY.plusDays(366), null))
            .isInstanceOf(CalendarException.EventTooLong.class);
    }

    @Test
    void aWeeklyOccurrenceMayFillItsWholeWeek() {
        assertThatCode(() -> allDaySeries(6, RecurrenceInterval.WEEKLY, 1)).doesNotThrowAnyException();
    }

    @Test
    void aWeeklyOccurrenceMayNotRunIntoTheNextOne() {
        assertThatThrownBy(() -> allDaySeries(7, RecurrenceInterval.WEEKLY, 1))
            .isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void aNightlyShiftRunningPastMidnightDoesNotOverlapTheNextNight() {
        assertThatCode(() -> timedSeries("22:00", "02:00", 1, RecurrenceInterval.DAILY)).doesNotThrowAnyException();
    }

    @Test
    void aTimedOccurrenceMayEndExactlyWhereTheNextBegins() {
        assertThatCode(() -> timedSeries("09:00", "09:00", 1, RecurrenceInterval.DAILY)).doesNotThrowAnyException();
    }

    @Test
    void aTimedOccurrenceMayNotOverlapTheNextByAQuarterOfAnHour() {
        assertThatThrownBy(() -> timedSeries("09:00", "09:15", 1, RecurrenceInterval.DAILY))
            .isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void aMonthIsReckonedAtItsShortest() {
        // February: a monthly occurrence of 28 days fits every month, one of 29 runs into March's.
        assertThatCode(() -> allDaySeries(27, RecurrenceInterval.MONTHLY, 1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> allDaySeries(28, RecurrenceInterval.MONTHLY, 1))
            .isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void refusesTheMillionDaySeriesThatFloodedEveryRead() {
        assertThatThrownBy(() -> allDaySeries(1_000_000, RecurrenceInterval.DAILY, 1))
            .isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void aVeryLongIntervalDoesNotOverflowWhileMeasuringIt() {
        assertThatCode(() -> allDaySeries(1_000_000, RecurrenceInterval.YEARLY, 10_000_000)).doesNotThrowAnyException();
    }

    private static void allDaySeries(int durationDays, RecurrenceInterval intervalType, int intervalCount) {
        EventScheduleValidator.validateSeries(true, null, null, durationDays, intervalType, intervalCount, DAY, null);
    }

    private static void timedSeries(String start, String end, int durationDays, RecurrenceInterval intervalType) {
        EventScheduleValidator.validateSeries(false, LocalTime.parse(start), LocalTime.parse(end), durationDays,
            intervalType, 1, DAY, null);
    }
}
