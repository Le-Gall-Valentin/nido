package com.nido.api.tasks.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

    @Test
    void validateSchedule_accepts_a_lead_time_within_the_recurrence_interval() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThatNoException().isThrownBy(() ->
            RecurrenceScheduler.validateSchedule(anchor, RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 3, null));
    }

    @Test
    void validateSchedule_rejects_a_lead_time_longer_than_the_recurrence_interval() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThatThrownBy(() ->
            RecurrenceScheduler.validateSchedule(anchor, RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 8, null))
            .isInstanceOf(TaskException.LeadTimeExceedsInterval.class);
    }

    @Test
    void validateSchedule_rejects_an_end_date_before_the_anchor_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThatThrownBy(() ->
            RecurrenceScheduler.validateSchedule(anchor, RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, anchor.minusDays(1)))
            .isInstanceOf(TaskException.InvalidEndDate.class);
    }

    @Test
    void validateSchedule_accepts_a_null_end_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThatNoException().isThrownBy(() ->
            RecurrenceScheduler.validateSchedule(anchor, RecurrenceInterval.WEEKLY, 1, RecurrenceInterval.DAILY, 0, null));
    }
    // ── borne du rattrapage (B1) ───────────────────────────────────────────

    @Test
    void the_starting_index_is_exact_when_month_end_clamping_is_in_play() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        assertThat(RecurrenceScheduler.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 2, 15))).isEqualTo(1L);
        assertThat(RecurrenceScheduler.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 3, 1))).isEqualTo(2L);
        assertThat(RecurrenceScheduler.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, anchor)).isZero();
    }

    @Test
    void the_pending_count_ignores_what_the_series_has_already_generated() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 11);

        // Occurrences 1..10 are due by today; occurrence 0 is the anchor task the handler
        // creates directly, which is why the count starts at 1.
        assertThat(RecurrenceScheduler.pendingOccurrenceCount(anchor, RecurrenceInterval.DAILY, 1, null, today, 0)).isEqualTo(10L);
        assertThat(RecurrenceScheduler.pendingOccurrenceCount(anchor, RecurrenceInterval.DAILY, 1, null, today, 7)).isEqualTo(3L);
        assertThat(RecurrenceScheduler.pendingOccurrenceCount(anchor, RecurrenceInterval.DAILY, 1, null, today, 10)).isZero();
    }

    @Test
    void the_pending_count_stops_at_the_end_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThat(RecurrenceScheduler.pendingOccurrenceCount(
            anchor, RecurrenceInterval.DAILY, 1, LocalDate.of(2026, 1, 4), LocalDate.of(2026, 6, 1), 0)).isEqualTo(3L);
    }

    // ── refus a la saisie (B1) ─────────────────────────────────────────────

    @Test
    void a_daily_series_anchored_years_back_is_refused_outright() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatThrownBy(() -> RecurrenceScheduler.validateBacklog(
            LocalDate.of(2001, 1, 1), RecurrenceInterval.DAILY, 1, null, today, 0))
            .isInstanceOf(TaskException.RecurrenceBacklogTooLarge.class);
    }

    @Test
    void back_dating_a_weekly_series_to_the_start_of_the_year_stays_allowed() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatNoException().isThrownBy(() -> RecurrenceScheduler.validateBacklog(
            LocalDate.of(2026, 1, 1), RecurrenceInterval.WEEKLY, 1, null, today, 0));
    }

    @Test
    void a_daily_series_is_allowed_right_up_to_the_ceiling_and_refused_one_past_it() {
        LocalDate today = LocalDate.of(2026, 9, 9);
        LocalDate exactlyAtCeiling = today.minusDays(RecurrenceScheduler.MAX_BACKLOG_OCCURRENCES);

        assertThatNoException().isThrownBy(() -> RecurrenceScheduler.validateBacklog(
            exactlyAtCeiling, RecurrenceInterval.DAILY, 1, null, today, 0));
        assertThatThrownBy(() -> RecurrenceScheduler.validateBacklog(
            exactlyAtCeiling.minusDays(1), RecurrenceInterval.DAILY, 1, null, today, 0))
            .isInstanceOf(TaskException.RecurrenceBacklogTooLarge.class);
    }

    @Test
    void a_long_running_series_that_kept_up_carries_no_backlog_and_can_still_be_edited() {
        // The case that would break every long-running series if the ceiling were measured
        // from the anchor: a daily series started in 2001 that has generated every occurrence
        // since owes nothing, so renaming it must stay possible.
        LocalDate anchor = LocalDate.of(2001, 1, 1);
        LocalDate today = LocalDate.of(2026, 9, 9);
        int generatedSoFar = (int) java.time.temporal.ChronoUnit.DAYS.between(anchor, today);

        assertThatNoException().isThrownBy(() -> RecurrenceScheduler.validateBacklog(
            anchor, RecurrenceInterval.DAILY, 1, null, today, generatedSoFar));
    }

    @Test
    void a_series_anchored_in_the_future_carries_no_backlog() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatNoException().isThrownBy(() -> RecurrenceScheduler.validateBacklog(
            LocalDate.of(2027, 1, 1), RecurrenceInterval.DAILY, 1, null, today, 0));
    }
    // ── non-régression : le comptage arithmétique doit rendre ce que rend un
    //    décompte naïf occurrence par occurrence ────────────────────────────

    /** Counts occurrences one at a time. Slow, but unquestionably right. */
    private static long byCountingOneByOne(LocalDate anchorDate, RecurrenceInterval intervalType, int intervalCount,
                                           LocalDate endDate, LocalDate today, int alreadyGeneratedCount) {
        long due = 0;
        for (int n = 1; n <= 20_000; n++) {
            LocalDate dueDate = RecurrenceScheduler.nextDueDate(anchorDate, intervalType, intervalCount, n);
            if (endDate != null && dueDate.isAfter(endDate)) break;
            if (dueDate.isAfter(today)) break;
            due++;
        }
        return Math.max(0L, due - alreadyGeneratedCount);
    }

    @Test
    void the_arithmetic_pending_count_agrees_with_counting_one_by_one() {
        List<LocalDate> anchors = List.of(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), LocalDate.of(2028, 2, 29),
            LocalDate.of(2025, 12, 31), LocalDate.of(2024, 6, 15), LocalDate.of(2027, 3, 10));
        List<LocalDate> todays = List.of(
            LocalDate.of(2024, 1, 1), LocalDate.of(2026, 2, 28), LocalDate.of(2026, 9, 9),
            LocalDate.of(2027, 1, 1), LocalDate.of(2029, 5, 20));
        List<LocalDate> endDates = new java.util.ArrayList<>();
        endDates.add(null);
        endDates.add(LocalDate.of(2026, 6, 30));

        for (LocalDate anchor : anchors) {
            for (RecurrenceInterval interval : RecurrenceInterval.values()) {
                for (int count = 1; count <= 4; count++) {
                    for (LocalDate today : todays) {
                        for (LocalDate endDate : endDates) {
                            for (int generated : new int[]{0, 3, 50}) {
                                assertThat(RecurrenceScheduler.pendingOccurrenceCount(anchor, interval, count, endDate, today, generated))
                                    .as("anchor=%s interval=%s x%d today=%s endDate=%s generated=%d",
                                        anchor, interval, count, today, endDate, generated)
                                    .isEqualTo(byCountingOneByOne(anchor, interval, count, endDate, today, generated));
                            }
                        }
                    }
                }
            }
        }
    }
}
