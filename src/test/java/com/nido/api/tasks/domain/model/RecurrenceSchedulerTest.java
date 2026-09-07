package com.nido.api.tasks.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceSchedulerTest {

    @Test
    void occurrence_zero_is_always_the_anchor_date_itself() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 1, 0)).isEqualTo(anchor);
    }

    @Test
    void daily_every_n_days() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.DAILY, 8, 1))
            .isEqualTo(LocalDate.of(2026, 1, 9));
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.DAILY, 8, 2))
            .isEqualTo(LocalDate.of(2026, 1, 17));
    }

    @Test
    void weekly_every_week() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.WEEKLY, 1, 1))
            .isEqualTo(LocalDate.of(2026, 1, 14));
    }

    @Test
    void monthly_simple_case_keeps_the_same_day_of_month() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 1, 1))
            .isEqualTo(LocalDate.of(2026, 2, 7));
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 1, 12))
            .isEqualTo(LocalDate.of(2027, 1, 7));
    }

    @Test
    void monthly_anchored_on_the_31st_clamps_to_the_shorter_month_without_losing_the_anchor_day() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        // 2026 is not a leap year: February has 28 days.
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 1, 1))
            .isEqualTo(LocalDate.of(2026, 2, 28));
        // The next occurrence still targets the 31st — computed fresh from the
        // anchor, not chained from the clamped February date.
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 1, 2))
            .isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void monthly_every_n_months() {
        LocalDate anchor = LocalDate.of(2026, 1, 15);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.MONTHLY, 3, 1))
            .isEqualTo(LocalDate.of(2026, 4, 15));
    }

    @Test
    void yearly_every_n_years() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.YEARLY, 1, 1))
            .isEqualTo(LocalDate.of(2027, 1, 7));
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.YEARLY, 2, 1))
            .isEqualTo(LocalDate.of(2028, 1, 7));
    }

    @Test
    void yearly_anchored_on_a_leap_day_clamps_to_feb_28_without_losing_the_anchor_day() {
        LocalDate anchor = LocalDate.of(2028, 2, 29);

        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.YEARLY, 1, 1))
            .isEqualTo(LocalDate.of(2029, 2, 28));
        // The next occurrence still targets Feb 29 — computed fresh from the
        // anchor, not chained from the clamped 2029 date.
        assertThat(RecurrenceScheduler.nextDueDate(anchor, RecurrenceInterval.YEARLY, 1, 4))
            .isEqualTo(LocalDate.of(2032, 2, 29));
    }

    @Test
    void minus_shifts_a_concrete_date_backward_by_the_given_interval() {
        assertThat(RecurrenceScheduler.minus(LocalDate.of(2026, 3, 15), RecurrenceInterval.DAILY, 8))
            .isEqualTo(LocalDate.of(2026, 3, 7));
        assertThat(RecurrenceScheduler.minus(LocalDate.of(2026, 3, 15), RecurrenceInterval.WEEKLY, 2))
            .isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(RecurrenceScheduler.minus(LocalDate.of(2026, 3, 15), RecurrenceInterval.MONTHLY, 1))
            .isEqualTo(LocalDate.of(2026, 2, 15));
        assertThat(RecurrenceScheduler.minus(LocalDate.of(2026, 3, 15), RecurrenceInterval.YEARLY, 1))
            .isEqualTo(LocalDate.of(2025, 3, 15));
    }

    @Test
    void minus_by_zero_returns_the_same_date_regardless_of_unit() {
        LocalDate date = LocalDate.of(2026, 3, 15);
        assertThat(RecurrenceScheduler.minus(date, RecurrenceInterval.YEARLY, 0)).isEqualTo(date);
    }

    @Test
    void minus_a_month_from_the_31st_clamps_to_the_shorter_month() {
        assertThat(RecurrenceScheduler.minus(LocalDate.of(2026, 3, 31), RecurrenceInterval.MONTHLY, 1))
            .isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
