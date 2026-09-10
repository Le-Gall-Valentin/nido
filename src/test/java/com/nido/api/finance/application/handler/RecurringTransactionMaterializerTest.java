package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionMaterializerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    @Test
    void advances_the_series_cursor_only_once_even_with_a_large_backlog() {
        RecurringTransactionSeries series = new RecurringTransactionSeries(seriesId, spaceId, "Abonnement",
            new BigDecimal("9.99"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.DAILY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));
        LocalDate today = LocalDate.of(2026, 1, 10);

        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, spaceId, today);

        verify(transactionRepository, times(10)).create(any(), any());
        // A wildcard matcher on the date so this counts every call regardless of which date was
        // passed — a verify(seriesId, today) here would only check the *last* of several calls
        // and pass even if the cursor were (wrongly) advanced once per occurrence instead of once.
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(seriesRepository, times(1)).advanceLastMaterializedDate(eq(seriesId), dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(today);
    }

    @Test
    void does_not_advance_the_cursor_when_the_series_is_already_fully_caught_up() {
        // lastMaterializedDate already sits on endDate — every occurrence this series will
        // ever produce is already materialized, so occurrencesBetween returns nothing.
        RecurringTransactionSeries series = new RecurringTransactionSeries(seriesId, spaceId, "Abonnement",
            new BigDecimal("9.99"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        RecurringTransactionMaterializer.materializeDueOccurrences(transactionRepository, seriesRepository, spaceId, LocalDate.of(2026, 6, 1));

        verify(seriesRepository, never()).advanceLastMaterializedDate(any(), any());
    }
    @Test
    void a_backlog_beyond_the_ceiling_is_capped_and_the_cursor_stops_where_it_stopped() {
        // Anchored far enough back that the uncapped loop would generate tens of thousands of
        // rows in one transaction — the failure mode this ceiling exists to prevent.
        LocalDate anchor = LocalDate.of(2000, 1, 1);
        RecurringTransactionSeries series = new RecurringTransactionSeries(seriesId, spaceId, "Abonnement",
            new BigDecimal("9.99"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.DAILY, 1, anchor, null, null);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        RecurringTransactionMaterializer.materializeDueOccurrences(
            transactionRepository, seriesRepository, spaceId, LocalDate.of(2026, 9, 9));

        verify(transactionRepository, times(RecurringTransactionMaterializer.MAX_OCCURRENCES_PER_RUN)).create(any(), any());
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(seriesRepository, times(1)).advanceLastMaterializedDate(eq(seriesId), dateCaptor.capture());
        // The cursor sits on the last occurrence actually written, so the next read resumes
        // from there rather than replaying or skipping any of them.
        assertThat(dateCaptor.getValue())
            .isEqualTo(anchor.plusDays(RecurringTransactionMaterializer.MAX_OCCURRENCES_PER_RUN - 1L));
    }

    @Test
    void successive_passes_keep_catching_up_from_where_the_previous_one_left_off() {
        LocalDate anchor = LocalDate.of(2000, 1, 1);
        LocalDate cursorAfterFirstPass = anchor.plusDays(RecurringTransactionMaterializer.MAX_OCCURRENCES_PER_RUN - 1L);
        RecurringTransactionSeries series = new RecurringTransactionSeries(seriesId, spaceId, "Abonnement",
            new BigDecimal("9.99"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.DAILY, 1, anchor, null, cursorAfterFirstPass);
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        RecurringTransactionMaterializer.materializeDueOccurrences(
            transactionRepository, seriesRepository, spaceId, LocalDate.of(2026, 9, 9));

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(seriesRepository, times(1)).advanceLastMaterializedDate(eq(seriesId), dateCaptor.capture());
        assertThat(dateCaptor.getValue())
            .isEqualTo(cursorAfterFirstPass.plusDays(RecurringTransactionMaterializer.MAX_OCCURRENCES_PER_RUN));
    }
}
