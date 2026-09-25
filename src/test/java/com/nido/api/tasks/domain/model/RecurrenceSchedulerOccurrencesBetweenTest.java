package com.nido.api.tasks.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The range query the calendar needs. Tasks materializes occurrences only up to today, so the
 * future of a recurring task exists nowhere until something projects it — this is that something.
 */
class RecurrenceSchedulerOccurrencesBetweenTest {

    @Test
    void returnsOnlyTheOccurrencesInsideTheWindow() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 1, 1), RecurrenceInterval.DAILY, 1, null,
            LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12), 100))
            .containsExactly(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 11), LocalDate.of(2026, 1, 12));
    }

    @Test
    void stopsAtTheLimitRatherThanRunningAway() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 1, 1), RecurrenceInterval.DAILY, 1, null,
            LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), 5)).hasSize(5);
    }

    @Test
    void neverGoesPastTheSeriesEndDate() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 1, 1), RecurrenceInterval.DAILY, 1, LocalDate.of(2026, 1, 3),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10), 100))
            .containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3));
    }

    @Test
    void neverReturnsAnythingBeforeTheAnchor() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 2, 1), RecurrenceInterval.WEEKLY, 1, null,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 10), 100))
            .containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 8));
    }

    @Test
    void clampsAMonthlyAnchorWithoutDrifting() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 1, 31), RecurrenceInterval.MONTHLY, 1, null,
            LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31), 100))
            .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31));
    }

    @Test
    void returnsNothingWhenTheWindowEndsBeforeTheAnchor() {
        assertThat(RecurrenceScheduler.occurrencesBetween(
            LocalDate.of(2026, 6, 1), RecurrenceInterval.DAILY, 1, null,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 100)).isEmpty();
    }
}
