package com.nido.api.finance.application.service;

import com.nido.api.finance.domain.model.OpeningBalance;
import com.nido.api.finance.domain.port.out.OpeningBalanceRepository;
import com.nido.api.finance.domain.port.out.RunningTotalsRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunningBalanceReaderTest {

    @Mock RunningTotalsRepository runningTotals;
    @Mock OpeningBalanceRepository openingBalances;
    @Mock TransactionRepository transactions;
    private final UUID spaceId = UUID.randomUUID();

    @Test
    void without_an_opening_balance_the_balance_is_the_month_nets_alone() {
        when(runningTotals.findNetByMonth(spaceId)).thenReturn(Map.of(YearMonth.of(2026, 9), new BigDecimal("70.00")));
        when(openingBalances.findBySpaceId(spaceId)).thenReturn(Optional.empty());

        BigDecimal balance = new RunningBalanceReader(runningTotals, openingBalances, transactions).read(spaceId)
            .atEndOf(YearMonth.of(2026, 12));

        assertThat(balance).isEqualByComparingTo("70.00");
        verify(transactions, never()).sumRecordedBetween(any(), any(), any());
    }

    @Test
    void an_opening_balance_is_anchored_with_what_its_month_recorded_up_to_its_day() {
        when(runningTotals.findNetByMonth(spaceId)).thenReturn(Map.of(YearMonth.of(2026, 10), new BigDecimal("500.00")));
        when(openingBalances.findBySpaceId(spaceId)).thenReturn(Optional.of(
            new OpeningBalance(spaceId, new BigDecimal("1000.00"), LocalDate.of(2026, 10, 11))));
        when(transactions.sumRecordedBetween(spaceId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11)))
            .thenReturn(new BigDecimal("200.00"));

        BigDecimal balance = new RunningBalanceReader(runningTotals, openingBalances, transactions).read(spaceId)
            .atEndOf(YearMonth.of(2026, 10));

        assertThat(balance).isEqualByComparingTo("1300.00");
    }
}
