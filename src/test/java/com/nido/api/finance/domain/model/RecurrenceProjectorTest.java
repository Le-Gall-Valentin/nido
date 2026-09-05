package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecurrenceProjectorTest {

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
            anchor, RecurrenceInterval.WEEKLY, 1, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(occurrences).containsExactly(
            LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28));
    }

    @Test
    void occurrences_between_can_project_several_months_ahead() {
        LocalDate anchor = LocalDate.of(2026, 1, 15);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, null, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(occurrences).containsExactly(LocalDate.of(2026, 6, 15));
    }

    @Test
    void occurrences_between_respects_an_end_date() {
        LocalDate anchor = LocalDate.of(2026, 1, 1);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThat(occurrences).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1));
    }

    @Test
    void occurrences_between_returns_empty_when_the_range_is_entirely_before_the_anchor() {
        LocalDate anchor = LocalDate.of(2026, 6, 1);

        List<LocalDate> occurrences = RecurrenceProjector.occurrencesBetween(
            anchor, RecurrenceInterval.MONTHLY, 1, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31));

        assertThat(occurrences).isEmpty();
    }
}
