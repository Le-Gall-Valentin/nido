package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

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

    private RecurringEventSeries series(int intervalCount, int durationDays, LocalDate anchor, LocalDate end) {
        return new RecurringEventSeries(
            UUID.randomUUID(), UUID.randomUUID(), "Piano", null, null,
            false, LocalTime.of(18, 0), LocalTime.of(19, 0), durationDays, null,
            RecurrenceInterval.WEEKLY, intervalCount, anchor, end, List.of(), UUID.randomUUID(), Instant.now());
    }
}
