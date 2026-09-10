package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecurrenceProjectorTest {

    @Test
    void an_interval_count_of_zero_or_less_is_rejected_instead_of_looping_forever() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 0, null, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31), 100))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void occurrence_zero_is_always_the_anchor_date_itself() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.MONTHLY, 1, 0)).isEqualTo(anchor);
    }

    @Test
    void monthly_anchored_on_the_31st_clamps_without_drifting() {
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.MONTHLY, 1, 1)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.MONTHLY, 1, 2)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void occurrences_between_returns_every_due_date_in_the_requested_range_inclusive() {
        LocalDate anchor = LocalDate.of(2026, 1, 7);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.WEEKLY, 1, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 100);

        assertThat(occurrences).containsExactly(
            LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28));
    }

    @Test
    void occurrences_between_can_project_several_months_ahead() {
        LocalDate anchor = LocalDate.of(2026, 1, 15);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 100);

        assertThat(occurrences).containsExactly(LocalDate.of(2026, 6, 15));
    }

    @Test
    void occurrences_between_respects_an_end_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 100);

        assertThat(occurrences).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
    }

    @Test
    void yearly_anchored_on_leap_day_clamps_without_drifting() {
        LocalDate anchor = LocalDate.of(2028, 2, 29);

        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.YEARLY, 1, 1)).isEqualTo(LocalDate.of(2029, 2, 28));
        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.YEARLY, 1, 4)).isEqualTo(LocalDate.of(2032, 2, 29));
    }

    @Test
    void yearly_supports_a_multi_year_step() {
        LocalDate anchor = LocalDate.of(2026, 1, 15);

        assertThat(RecurrenceProjector.occurrenceDate(anchor, RecurrenceInterval.YEARLY, 2, 1)).isEqualTo(LocalDate.of(2028, 1, 15));
    }

    @Test
    void occurrences_between_returns_empty_when_the_range_is_entirely_before_the_anchor() {
        LocalDate anchor = LocalDate.of(2026, 6, 1);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31), 100);

        assertThat(occurrences).isEmpty();
    }
    // ── borne du rattrapage (B1) ───────────────────────────────────────────

    @Test
    void occurrences_between_stops_at_the_requested_limit() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.DAILY, 1, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), 3);

        assertThat(occurrences).containsExactly(
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3));
    }

    @Test
    void occurrences_between_lands_on_the_right_dates_however_old_the_anchor_is() {
        // The whole point of computing the starting index arithmetically: an anchor a century
        // back must cost the same as a recent one, and must still land on the exact dates.
        LocalDate anchor = LocalDate.of(1926, 3, 4);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.DAILY, 1, null, LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 11), 100);

        assertThat(occurrences).containsExactly(
            LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 11));
    }

    @Test
    void the_starting_index_is_exact_when_month_end_clamping_is_in_play() {
        // A Jan-31 anchor produces Feb 28 then Mar 31, so the arithmetic estimate (which only
        // knows calendar months) has to be corrected by the two guard steps.
        LocalDate anchor = LocalDate.of(2026, 1, 31);

        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 2, 15))).isEqualTo(1L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 2, 28))).isEqualTo(1L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 3, 1))).isEqualTo(2L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.MONTHLY, 1, anchor)).isZero();
    }

    @Test
    void the_starting_index_is_exact_for_every_interval_and_step() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.DAILY, 3, LocalDate.of(2026, 1, 10))).isEqualTo(3L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.DAILY, 3, LocalDate.of(2026, 1, 9))).isEqualTo(3L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.WEEKLY, 2, LocalDate.of(2026, 1, 20))).isEqualTo(2L);
        assertThat(RecurrenceProjector.firstOccurrenceIndexOnOrAfter(anchor, RecurrenceInterval.YEARLY, 1, LocalDate.of(2029, 6, 1))).isEqualTo(4L);
    }

    @Test
    void occurrence_count_between_agrees_with_the_dates_actually_produced() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        long counted = RecurrenceProjector.occurrenceCountBetween(anchor, RecurrenceInterval.WEEKLY, 1, null, from, to);

        assertThat(counted).isEqualTo(
            RecurrenceProjector.occurrencesBetween(anchor, RecurrenceInterval.WEEKLY, 1, null, from, to, 1000).size());
    }

    @Test
    void occurrence_count_between_stops_at_the_end_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        assertThat(RecurrenceProjector.occurrenceCountBetween(anchor, RecurrenceInterval.MONTHLY, 1,
            LocalDate.of(2026, 3, 1), anchor, LocalDate.of(2026, 12, 31))).isEqualTo(3L);
    }

    // ── refus a la saisie (B1) ─────────────────────────────────────────────

    @Test
    void a_daily_series_anchored_years_back_is_refused_outright() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatThrownBy(() -> RecurrenceProjector.validateBacklog(
            LocalDate.of(2001, 1, 1), RecurrenceInterval.DAILY, 1, null, today, null))
            .isInstanceOf(FinanceException.RecurrenceBacklogTooLarge.class);
    }

    @Test
    void back_dating_a_monthly_series_to_the_start_of_the_year_stays_allowed() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatCode(() -> RecurrenceProjector.validateBacklog(
            LocalDate.of(2026, 1, 1), RecurrenceInterval.MONTHLY, 1, null, today, null))
            .doesNotThrowAnyException();
    }

    @Test
    void a_daily_series_is_allowed_right_up_to_the_ceiling_and_refused_one_past_it() {
        LocalDate today = LocalDate.of(2026, 9, 9);
        LocalDate exactlyAtCeiling = today.minusDays(RecurrenceProjector.MAX_BACKLOG_OCCURRENCES - 1L);

        assertThatCode(() -> RecurrenceProjector.validateBacklog(
            exactlyAtCeiling, RecurrenceInterval.DAILY, 1, null, today, null))
            .doesNotThrowAnyException();
        assertThatThrownBy(() -> RecurrenceProjector.validateBacklog(
            exactlyAtCeiling.minusDays(1), RecurrenceInterval.DAILY, 1, null, today, null))
            .isInstanceOf(FinanceException.RecurrenceBacklogTooLarge.class);
    }

    @Test
    void an_old_series_already_caught_up_carries_no_backlog_and_can_still_be_edited() {
        // The case that would break every long-running series if the ceiling were measured
        // from the anchor: a daily series started in 2001 but materialized up to yesterday
        // owes exactly one occurrence, not nine thousand.
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatCode(() -> RecurrenceProjector.validateBacklog(
            LocalDate.of(2001, 1, 1), RecurrenceInterval.DAILY, 1, null, today, today.minusDays(1)))
            .doesNotThrowAnyException();
    }

    @Test
    void a_series_anchored_in_the_future_carries_no_backlog() {
        LocalDate today = LocalDate.of(2026, 9, 9);

        assertThatCode(() -> RecurrenceProjector.validateBacklog(
            LocalDate.of(2027, 1, 1), RecurrenceInterval.DAILY, 1, null, today, null))
            .doesNotThrowAnyException();
    }
}
