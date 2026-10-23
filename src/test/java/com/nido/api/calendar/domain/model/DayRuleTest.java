package com.nido.api.calendar.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class DayRuleTest {

    @Test
    void theSameDayIsCappedAtTheEndOfAShorterMonth() {
        LocalDate the31st = LocalDate.of(2026, 1, 31);
        assertThat(DayRule.SAME_DAY.in(YearMonth.of(2026, 2), the31st)).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(DayRule.SAME_DAY.in(YearMonth.of(2026, 3), the31st)).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void theNthWeekdayIsReadOffTheAnchor() {
        // 13 Oct 2026 is the second Tuesday of October.
        LocalDate secondTuesday = LocalDate.of(2026, 10, 13);
        assertThat(DayRule.WEEKDAY_OF_MONTH.in(YearMonth.of(2026, 11), secondTuesday)).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(DayRule.WEEKDAY_OF_MONTH.in(YearMonth.of(2026, 12), secondTuesday)).isEqualTo(LocalDate.of(2026, 12, 8));
    }

    @Test
    void theFourthWeekdayExistsInEveryMonth() {
        LocalDate fourthSaturday = LocalDate.of(2026, 10, 24);
        assertThat(DayRule.WEEKDAY_OF_MONTH.in(YearMonth.of(2027, 2), fourthSaturday)).isEqualTo(LocalDate.of(2027, 2, 27));
    }

    @Test
    void theLastWeekdayIsTheLastOfItsMonth() {
        LocalDate lastFriday = LocalDate.of(2026, 10, 30);
        assertThat(DayRule.LAST_WEEKDAY_OF_MONTH.in(YearMonth.of(2026, 11), lastFriday)).isEqualTo(LocalDate.of(2026, 11, 27));
        assertThat(DayRule.LAST_WEEKDAY_OF_MONTH.in(YearMonth.of(2027, 1), lastFriday)).isEqualTo(LocalDate.of(2027, 1, 29));
    }

    @Test
    void theLastDayFollowsTheLengthOfEachMonth() {
        LocalDate april30 = LocalDate.of(2026, 4, 30);
        assertThat(DayRule.LAST_DAY_OF_MONTH.in(YearMonth.of(2026, 5), april30)).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(DayRule.LAST_DAY_OF_MONTH.in(YearMonth.of(2028, 2), april30)).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void eachRuleFitsOnlyTheAnchorsItCanBeReadOff() {
        assertThat(DayRule.SAME_DAY.fits(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(DayRule.WEEKDAY_OF_MONTH.fits(LocalDate.of(2026, 10, 28))).as("a 4th").isTrue();
        assertThat(DayRule.WEEKDAY_OF_MONTH.fits(LocalDate.of(2026, 10, 29))).as("a 5th").isFalse();
        assertThat(DayRule.LAST_WEEKDAY_OF_MONTH.fits(LocalDate.of(2026, 10, 25))).isTrue();
        assertThat(DayRule.LAST_WEEKDAY_OF_MONTH.fits(LocalDate.of(2026, 10, 24))).isFalse();
        assertThat(DayRule.LAST_DAY_OF_MONTH.fits(LocalDate.of(2026, 2, 28))).isTrue();
        assertThat(DayRule.LAST_DAY_OF_MONTH.fits(LocalDate.of(2028, 2, 28))).as("a leap February").isFalse();
    }
}
