package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurringEventSeriesTest {

    @Test
    void refusesAZeroOrNegativeInterval() {
        assertThatThrownBy(() -> series(0, 0, LocalDate.of(2026, 1, 1), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("intervalCount");
    }

    @Test
    void refusesANegativeDuration() {
        assertThatThrownBy(() -> series(1, -1, LocalDate.of(2026, 1, 1), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("durationDays");
    }

    @Test
    void refusesAnEndDateBeforeTheAnchor() {
        assertThatThrownBy(() -> series(1, 0, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endDate");
    }

    @Test
    void acceptsAnEndDateOnTheAnchorItself() {
        assertThatCode(() -> series(1, 0, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)))
            .doesNotThrowAnyException();
    }

    @Test
    void acceptsAMultiDayRecurringSeries() {
        assertThatCode(() -> series(1, 3, LocalDate.of(2026, 1, 1), null))
            .doesNotThrowAnyException();
    }

    @Test
    void aWeeklySeriesGivenNoDaysRepeatsOnItsAnchorsDay() {
        // 2026-01-06 is a Tuesday.
        assertThat(series(1, 0, LocalDate.of(2026, 1, 6), null).weekdays()).containsExactly(DayOfWeek.TUESDAY);
    }

    @Test
    void aWeeklySeriesMustRepeatOnItsAnchorsDay() {
        assertThatThrownBy(() -> rule(RecurrenceInterval.WEEKLY, Set.of(DayOfWeek.MONDAY), null, LocalDate.of(2026, 1, 6)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("weekday");
    }

    @Test
    void onlyAWeeklySeriesHasDays() {
        assertThatThrownBy(() -> rule(RecurrenceInterval.MONTHLY, Set.of(DayOfWeek.TUESDAY), null, LocalDate.of(2026, 1, 6)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(rule(RecurrenceInterval.MONTHLY, null, null, LocalDate.of(2026, 1, 6)).weekdays()).isNull();
    }

    @Test
    void aSeriesGivenNoDayRuleKeepsTheSameDay() {
        assertThat(rule(RecurrenceInterval.MONTHLY, null, null, LocalDate.of(2026, 1, 6)).dayRule()).isEqualTo(DayRule.SAME_DAY);
    }

    @Test
    void aDayRuleBelongsToMonthlyAndYearlySeriesOnly() {
        assertThatThrownBy(() -> rule(RecurrenceInterval.WEEKLY, null, DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2026, 1, 31)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aDayRuleMustFitTheAnchor() {
        assertThatThrownBy(() -> rule(RecurrenceInterval.MONTHLY, null, DayRule.LAST_DAY_OF_MONTH, LocalDate.of(2026, 1, 30)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> rule(RecurrenceInterval.YEARLY, null, DayRule.LAST_WEEKDAY_OF_MONTH, LocalDate.of(2026, 5, 31)))
            .doesNotThrowAnyException();
    }

    private RecurringEventSeries rule(RecurrenceInterval interval, Set<DayOfWeek> weekdays, DayRule dayRule, LocalDate anchor) {
        return new RecurringEventSeries(
            UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            true, null, null, 0, null, interval, 1, weekdays, dayRule, anchor, null, null,
            List.of(), UUID.randomUUID(), Instant.now());
    }

    private RecurringEventSeries series(int intervalCount, int durationDays, LocalDate anchor, LocalDate end) {
        return new RecurringEventSeries(
            UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            false, LocalTime.of(18, 0), LocalTime.of(19, 0), durationDays, null,
            RecurrenceInterval.WEEKLY, intervalCount, anchor, end, List.of(), UUID.randomUUID(), Instant.now());
    }
}
