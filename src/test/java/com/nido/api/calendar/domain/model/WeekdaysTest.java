package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeekdaysTest {

    @Test
    void codesAreWrittenFromMondayOnWhateverTheOrderGiven() {
        assertThat(Weekdays.toCodes(Weekdays.copyOf(List.of(DayOfWeek.THURSDAY, DayOfWeek.MONDAY)))).isEqualTo("MO,TH");
    }

    @Test
    void codesAreReadBack() {
        assertThat(Weekdays.fromCodes("MO,TH,SU")).containsExactly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void nothingStaysNothing() {
        assertThat(Weekdays.toCodes(null)).isNull();
        assertThat(Weekdays.fromCodes(null)).isNull();
    }

    @Test
    void aWeeklySeriesGivenNoDaysRepeatsOnItsAnchorsDayAndOtherwiseOnThoseGiven() {
        // 2026-01-06 is a Tuesday.
        LocalDate tuesday = LocalDate.of(2026, 1, 6);
        assertThat(Weekdays.forWeekly(null, tuesday)).containsExactly(DayOfWeek.TUESDAY);
        assertThat(Weekdays.forWeekly(List.of(), tuesday)).containsExactly(DayOfWeek.TUESDAY);
        assertThat(Weekdays.forWeekly(List.of(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY), tuesday))
            .containsExactly(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY);
    }

    @Test
    void anUnknownCodeIsRefused() {
        assertThatThrownBy(() -> Weekdays.fromCode("XX")).isInstanceOf(IllegalArgumentException.class);
    }
}
