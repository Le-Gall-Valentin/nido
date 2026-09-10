package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.Projection;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProjectionHandlerTest {

    @Mock TransactionRepository transactionRepository;
    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private GetProjectionHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new GetProjectionHandler(transactionRepository, seriesRepository);
    }

    private SpaceMembership membership() {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    }

    @Test
    void combines_the_actual_balance_so_far_with_upcoming_recurring_occurrences_still_due_this_month() {
        LocalDate today = LocalDate.of(2026, 1, 10);
        Transaction soFar = new Transaction(UUID.randomUUID(), spaceId, "Courses", new BigDecimal("50.00"),
            TransactionType.EXPENSE, categoryId, LocalDate.of(2026, 1, 5), null, List.of(), null, Instant.now());
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of());
        when(transactionRepository.findBySpaceIdAndMonth(spaceId, YearMonth.of(2026, 1))).thenReturn(List.of(soFar));
        RecurringTransactionSeries rent = new RecurringTransactionSeries(UUID.randomUUID(), spaceId, "Loyer",
            new BigDecimal("800.00"), TransactionType.EXPENSE, categoryId, null, List.of(),
            RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 28), null, null);
        when(seriesRepository.findActiveBySpaceId(spaceId, today.plusDays(1))).thenReturn(List.of(rent));

        Projection projection = handler.getProjection(YearMonth.of(2026, 1), membership(), today);

        assertThat(projection.actualBalanceSoFar()).isEqualByComparingTo("-50.00");
        assertThat(projection.upcoming()).containsExactly(
            new ProjectedOccurrence(rent.id(), "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE, LocalDate.of(2026, 1, 28)));
        assertThat(projection.projectedEndOfMonthBalance()).isEqualByComparingTo("-850.00");
        // No series owes anything, so the pre-check spares the lock — the projection itself
        // reads future occurrences in memory and never needed it.
        verify(seriesRepository, never()).lockForMaterialization(spaceId);
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
