package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringSeriesSchedule;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The calendar shows a recurring operation's past and today from its real transactions, and the
 * rest from the projection. The materializer writes the first and moves the series' cursor; the
 * projection reads the cursor. This runs the real pair end to end: every date comes out once.
 */
class RecurringTransactionOccurrencesOnceTest {

    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    @Test
    void everyDateOfASeriesIsEitherATransactionOrProjectedNeverBoth() {
        RecurringTransactionSeries created = weekly(null);

        TransactionRepository transactions = mock(TransactionRepository.class);
        RecurringTransactionSeriesRepository seriesRepository = mock(RecurringTransactionSeriesRepository.class);
        when(seriesRepository.findSchedulesBySpaceId(spaceId)).thenReturn(List.of(scheduleOf(created)));
        when(seriesRepository.findBySpaceId(spaceId)).thenReturn(List.of(created));
        RecurringTransactionMaterializer.materializeDueOccurrences(transactions, seriesRepository, spaceId, LocalDate.of(2026, 1, 21));

        ArgumentCaptor<CreateTransactionCommand> written = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(transactions, atLeastOnce()).create(written.capture(), any());
        List<LocalDate> realTransactions = new ArrayList<>(written.getAllValues().stream().map(CreateTransactionCommand::date).toList());
        ArgumentCaptor<LocalDate> cursor = ArgumentCaptor.forClass(LocalDate.class);
        verify(seriesRepository).advanceLastMaterializedDate(eq(seriesId), cursor.capture());

        RecurringTransactionSeriesRepository advanced = mock(RecurringTransactionSeriesRepository.class);
        when(advanced.findActiveBySpaceId(eq(spaceId), any())).thenReturn(List.of(weekly(cursor.getValue())));
        SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
        List<LocalDate> projected = new ProjectRecurringSeriesHandler(advanced)
            .project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28)).stream()
            .map(ProjectedOccurrence::date).toList();

        List<LocalDate> shown = new ArrayList<>(realTransactions);
        shown.addAll(projected);
        assertThat(shown).doesNotHaveDuplicates();
        assertThat(shown).containsExactlyInAnyOrder(
            LocalDate.of(2026, 1, 7), LocalDate.of(2026, 1, 14), LocalDate.of(2026, 1, 21), LocalDate.of(2026, 1, 28),
            LocalDate.of(2026, 2, 4), LocalDate.of(2026, 2, 11), LocalDate.of(2026, 2, 18), LocalDate.of(2026, 2, 25));
    }

    private RecurringTransactionSeries weekly(LocalDate lastMaterialized) {
        return new RecurringTransactionSeries(seriesId, spaceId, "Abonnement", new BigDecimal("9.99"), TransactionType.EXPENSE,
            UUID.randomUUID(), null, List.of(), RecurrenceInterval.WEEKLY, 1, LocalDate.of(2026, 1, 7), null, lastMaterialized);
    }

    private static RecurringSeriesSchedule scheduleOf(RecurringTransactionSeries s) {
        return new RecurringSeriesSchedule(s.id(), s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(),
            s.lastMaterializedDate());
    }
}
