package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RunningBalanceTest {

    private static final UUID SPACE = UUID.randomUUID();

    private static BigDecimal amount(String value) {
        return new BigDecimal(value);
    }

    @Test
    void a_month_ends_on_every_month_before_it_and_its_own() {
        RunningBalance running = RunningBalance.of(Map.of(
            YearMonth.of(2026, 8), amount("100.00"), YearMonth.of(2026, 9), amount("-30.00"), YearMonth.of(2026, 11), amount("50.00")));

        assertThat(running.atEndOf(YearMonth.of(2026, 7))).isEqualByComparingTo("0");
        assertThat(running.atEndOf(YearMonth.of(2026, 9))).isEqualByComparingTo("70.00");
        assertThat(running.atEndOf(YearMonth.of(2026, 10))).as("a month with nothing in it inherits").isEqualByComparingTo("70.00");
        assertThat(running.atEndOf(YearMonth.of(2027, 2))).isEqualByComparingTo("120.00");
    }

    @Test
    void a_day_adds_what_its_month_recorded_up_to_it_and_nothing_after() {
        RunningBalance running = RunningBalance.of(Map.of(
            YearMonth.of(2026, 9), amount("70.00"), YearMonth.of(2026, 10), amount("500.00")));

        assertThat(running.at(LocalDate.of(2026, 10, 11), amount("200.00"))).isEqualByComparingTo("270.00");
    }

    @Test
    void an_opening_balance_is_the_balance_at_the_end_of_its_day() {
        // September −30; October 500 in all, 200 of it up to the 11th. The bank shows 1 000 at the end of the 11th.
        RunningBalance running = RunningBalance.of(Map.of(YearMonth.of(2026, 9), amount("-30.00"), YearMonth.of(2026, 10), amount("500.00")))
            .anchoredAt(new OpeningBalance(SPACE, amount("1000.00"), LocalDate.of(2026, 10, 11)), amount("200.00"));

        assertThat(running.at(LocalDate.of(2026, 10, 11), amount("200.00"))).isEqualByComparingTo("1000.00");
        assertThat(running.atEndOf(YearMonth.of(2026, 10))).as("what October recorded after the 11th comes on top").isEqualByComparingTo("1300.00");
        assertThat(running.atEndOf(YearMonth.of(2026, 9))).as("before the anchor: the balance it must have been").isEqualByComparingTo("800.00");
        assertThat(running.atEndOf(YearMonth.of(2027, 1))).isEqualByComparingTo("1300.00");
    }

    @Test
    void an_opening_balance_may_be_an_overdraft() {
        RunningBalance running = RunningBalance.of(Map.of())
            .anchoredAt(new OpeningBalance(SPACE, amount("-150.50"), LocalDate.of(2026, 10, 1)), BigDecimal.ZERO);

        assertThat(running.atEndOf(YearMonth.of(2026, 12))).isEqualByComparingTo("-150.50");
    }

    @Test
    void an_opening_balance_dated_after_the_last_operation_holds_from_then_on() {
        RunningBalance running = RunningBalance.of(Map.of(YearMonth.of(2026, 8), amount("100.00")))
            .anchoredAt(new OpeningBalance(SPACE, amount("40.00"), LocalDate.of(2026, 10, 11)), BigDecimal.ZERO);

        assertThat(running.atEndOf(YearMonth.of(2026, 8))).isEqualByComparingTo("40.00");
        assertThat(running.atEndOf(YearMonth.of(2026, 7))).as("before the August income").isEqualByComparingTo("-60.00");
        assertThat(running.atEndOf(YearMonth.of(2027, 1))).isEqualByComparingTo("40.00");
    }
}
