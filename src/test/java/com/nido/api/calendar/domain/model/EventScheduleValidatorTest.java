package com.nido.api.calendar.domain.model;

import com.nido.api.calendar.domain.model.CalendarException.InvalidRecurrenceRule.Reason;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    void aSeriesMayRepeatEveryThousandYearsButNoMoreRarely() {
        // Every billion years, a series reached past the last date a calendar can hold, and every read of its space failed.
        assertThatCode(() -> allDaySeries(0, RecurrenceInterval.YEARLY, 1000)).doesNotThrowAnyException();
        assertThatThrownBy(() -> allDaySeries(0, RecurrenceInterval.YEARLY, 1001))
            .isInstanceOfSatisfying(CalendarException.IntervalTooLarge.class,
                refused -> assertThat(refused.maximum()).isEqualTo(1000));
        assertThatThrownBy(() -> allDaySeries(0, RecurrenceInterval.DAILY, 1_000_000_000))
            .isInstanceOf(CalendarException.IntervalTooLarge.class);
    }

    @Test
    void anOccurrenceLastsNoLongerThanAnEvent() {
        // Every two years, an occurrence of two years would not run into the next one: it is still more than a read shows.
        assertThatCode(() -> allDaySeries(365, RecurrenceInterval.YEARLY, 2)).doesNotThrowAnyException();
        assertThatThrownBy(() -> allDaySeries(366, RecurrenceInterval.YEARLY, 2))
            .isInstanceOfSatisfying(CalendarException.EventTooLong.class,
                refused -> assertThat(refused.maximumDays()).isEqualTo(366));
        assertThatThrownBy(() -> EventScheduleValidator.validateSeries(false, LocalTime.of(9, 0), LocalTime.of(10, 0),
            366, RecurrenceInterval.YEARLY, 1000, DAY, null)).isInstanceOf(CalendarException.EventTooLong.class);
    }

    private static void allDaySeries(int durationDays, RecurrenceInterval intervalType, int intervalCount) {
        EventScheduleValidator.validateSeries(true, null, null, durationDays, intervalType, intervalCount, DAY, null);
    }

    private static void timedSeries(String start, String end, int durationDays, RecurrenceInterval intervalType) {
        EventScheduleValidator.validateSeries(false, LocalTime.parse(start), LocalTime.parse(end), durationDays,
            intervalType, 1, DAY, null);
    }

    @Test
    void anOccurrenceOnSeveralWeekdaysMayNotRunIntoTheNextDayChosen() {
        // Monday and Thursday: three days between them at the shortest.
        assertThatCode(() -> ruledAllDay(2, RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
            LocalDate.of(2026, 1, 5))).doesNotThrowAnyException();
        assertThatThrownBy(() -> ruledAllDay(3, RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), null,
            LocalDate.of(2026, 1, 5))).isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void theGapAcrossTwoPeriodsCountsToo() {
        // Monday and Saturday every week: Saturday to Monday is two days.
        assertThatThrownBy(() -> ruledAllDay(2, RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.MONDAY, DayOfWeek.SATURDAY), null,
            LocalDate.of(2026, 1, 5))).isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void aYearlyWeekdayComesBackAfter364DaysAtTheShortest() {
        assertThatCode(() -> ruledAllDay(363, RecurrenceInterval.YEARLY, null, DayRule.WEEKDAY_OF_MONTH,
            LocalDate.of(2026, 11, 26))).doesNotThrowAnyException();
        assertThatThrownBy(() -> ruledAllDay(364, RecurrenceInterval.YEARLY, null, DayRule.WEEKDAY_OF_MONTH,
            LocalDate.of(2026, 11, 26))).isInstanceOf(CalendarException.OccurrenceLongerThanInterval.class);
    }

    @Test
    void aRuleTheAnchorCannotGiveIsRefused() {
        assertThatThrownBy(() -> ruledAllDay(0, RecurrenceInterval.MONTHLY, null, DayRule.LAST_DAY_OF_MONTH,
            LocalDate.of(2026, 1, 30))).satisfies(refusedFor(Reason.DAY_RULE_NOT_FROM_ANCHOR));
        assertThatThrownBy(() -> ruledAllDay(0, RecurrenceInterval.MONTHLY, null, DayRule.WEEKDAY_OF_MONTH,
            LocalDate.of(2026, 1, 29))).satisfies(refusedFor(Reason.DAY_RULE_NOT_FROM_ANCHOR));
    }

    @Test
    void weekdaysWithoutTheAnchorsOrOutsideAWeeklySeriesAreRefused() {
        assertThatThrownBy(() -> ruledAllDay(0, RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.TUESDAY), null,
            LocalDate.of(2026, 1, 5))).satisfies(refusedFor(Reason.ANCHOR_NOT_AMONG_WEEKDAYS));
        assertThatThrownBy(() -> ruledAllDay(0, RecurrenceInterval.DAILY, Set.of(DayOfWeek.MONDAY), null,
            LocalDate.of(2026, 1, 5))).satisfies(refusedFor(Reason.WEEKDAYS_NOT_WEEKLY));
    }

    @Test
    void aDayRuleOutsideMonthsAndYearsIsRefused() {
        assertThatThrownBy(() -> ruledAllDay(0, RecurrenceInterval.WEEKLY, null, DayRule.LAST_DAY_OF_MONTH,
            LocalDate.of(2026, 1, 31))).satisfies(refusedFor(Reason.DAY_RULE_NOT_MONTHLY_OR_YEARLY));
    }

    private static ThrowingConsumer<Throwable> refusedFor(Reason reason) {
        return refused -> assertThat(refused).isInstanceOfSatisfying(CalendarException.InvalidRecurrenceRule.class,
            invalid -> assertThat(invalid.reason()).isEqualTo(reason));
    }

    private static void ruledAllDay(int durationDays, RecurrenceInterval interval, Set<DayOfWeek> weekdays,
                                    DayRule dayRule, LocalDate anchor) {
        EventScheduleValidator.validateSeries(true, null, null, durationDays, interval, 1, weekdays, dayRule, anchor, null);
    }
}
