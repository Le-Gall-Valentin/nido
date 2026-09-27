package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringSeriesSchedule;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.application.port.in.GetSpaceTodayUseCase;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterializeDueRecurringTransactionsHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock RecurringTransactionSeriesRepository seriesRepository;
    @Mock GetSpaceTodayUseCase spaceToday;

    private MaterializeDueRecurringTransactionsHandler handler;
    private final UUID spaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new MaterializeDueRecurringTransactionsHandler(transactionRepository, seriesRepository, spaceToday);
    }

    @Test
    void creates_every_occurrence_due_up_to_today_under_the_space_lock() {
        RecurringTransactionSeries series = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Abonnement",
            new BigDecimal("9.99"), TransactionType.EXPENSE, UUID.randomUUID(), null, List.of(),
            RecurrenceInterval.DAILY, 1, LocalDate.of(2026, 1, 1), null, null);
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of(scheduleOf(series)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(series));

        handler.materialize(membership(), LocalDate.of(2026, 1, 3));

        verify(seriesRepository).lockForMaterialization(spaceId);
        // Anchored on the 1st, daily, today the 3rd: the 1st, the 2nd and the 3rd.
        verify(transactionRepository, times(3)).create(any(), any());
    }

    @Test
    void takes_no_lock_and_writes_nothing_when_nothing_is_due() {
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());

        handler.materialize(membership(), LocalDate.of(2026, 1, 3));

        verify(seriesRepository, never()).lockForMaterialization(spaceId);
        verify(transactionRepository, never()).create(any(), any());
    }

    @Test
    void uses_the_space_own_today_when_called_without_one() {
        when(spaceToday.today(spaceId)).thenReturn(LocalDate.of(2026, 1, 3));
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());

        handler.materialize(membership());

        verify(spaceToday).today(spaceId);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());
    }

    private static RecurringSeriesSchedule scheduleOf(RecurringTransactionSeries s) {
        return new RecurringSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(),
            s.anchorDate(), s.endDate(), s.lastMaterializedDate());
    }
}
