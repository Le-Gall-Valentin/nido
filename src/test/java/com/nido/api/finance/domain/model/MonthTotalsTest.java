package com.nido.api.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MonthTotalsTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();
    private static final YearMonth JANUARY = YearMonth.of(2026, 1);

    @Test
    void the_net_counts_every_operation_and_the_members_only_the_shared_ones() {
        SplitTransaction groceries = new SplitTransaction(null, new BigDecimal("50.00"), List.of(), TransactionType.EXPENSE);
        SplitTransaction salary = new SplitTransaction(ALICE, new BigDecimal("2000.00"), List.of(), TransactionType.INCOME);
        SplitTransaction dinner = new SplitTransaction(ALICE, new BigDecimal("40.00"),
            List.of(new Contribution(ALICE, new BigDecimal("20.00")), new Contribution(BOB, new BigDecimal("20.00"))),
            TransactionType.EXPENSE);

        MonthTotals totals = MonthTotals.of(JANUARY, List.of(groceries, salary, dinner), List.of());

        assertThat(totals.net()).isEqualByComparingTo("1910.00");
        assertThat(totals.netByMember()).containsOnlyKeys(ALICE, BOB);
        assertThat(totals.netByMember().get(ALICE)).isEqualByComparingTo("20.00");
        assertThat(totals.netByMember().get(BOB)).isEqualByComparingTo("-20.00");
    }

    @Test
    void a_settlement_moves_the_members_and_not_the_net() {
        SettlementRecord settled = new SettlementRecord(UUID.randomUUID(), UUID.randomUUID(), BOB, ALICE,
            new BigDecimal("20.00"), LocalDate.of(2026, 1, 2));

        MonthTotals totals = MonthTotals.of(JANUARY, List.of(), List.of(settled));

        assertThat(totals.net()).isEqualByComparingTo("0");
        assertThat(totals.netByMember().get(BOB)).isEqualByComparingTo("20.00");
        assertThat(totals.netByMember().get(ALICE)).isEqualByComparingTo("-20.00");
    }
}
