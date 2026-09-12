package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringSeriesSchedule;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTransactionsHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private ListTransactionsHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListTransactionsHandler(transactionRepository, seriesRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void materializes_a_due_occurrence_before_listing_and_advances_the_series_cursor() {
        RecurringTransactionSeries series = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(series)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of());

        handler.list(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 1));

        verify(transactionRepository).create(new CreateTransactionCommand(spaceId, "Loyer", new BigDecimal("800.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 1), null, List.of(), series.id()), List.of());
        verify(seriesRepository).advanceLastMaterializedDate(series.id(), LocalDate.of(2026, 1, 1));
        verify(seriesRepository).lockForMaterialization(spaceId);
    }

    @Test
    void does_not_rematerialize_an_occurrence_already_covered_by_the_series_cursor() {
        RecurringTransactionSeries series = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1));
        when(seriesRepository.findSchedulesBySpaceId(spaceId))
            .thenReturn(List.of(scheduleOf(series)));
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of());

        handler.list(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 1));

        verify(transactionRepository, never()).create(any(), any());
    }

    @Test
    void returns_the_transactions_for_the_requested_month() {
        Transaction transaction = new Transaction(UUID.randomUUID(), spaceId, "T", new BigDecimal("10.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 15), null, List.of(), null, Instant.now());
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of(transaction));

        List<Transaction> result = handler.list(YearMonth.of(2026, 1), membership(), LocalDate.of(2026, 1, 20));

        assertThat(result).containsExactly(transaction);
    }
    /**
     * Derives the schedule projection from the full series, so a test cannot stub the two reads
     * with values that disagree — the pre-check and the materialization loop must see the same
     * series.
     */
    private static RecurringSeriesSchedule scheduleOf(RecurringTransactionSeries s) {
        return new RecurringSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(),
            s.anchorDate(), s.endDate(), s.lastMaterializedDate());
    }
}
