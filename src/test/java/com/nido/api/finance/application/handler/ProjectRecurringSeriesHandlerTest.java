package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.ProjectedOccurrence;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectRecurringSeriesHandlerTest {

    private final RecurringTransactionSeriesRepository series = mock(RecurringTransactionSeriesRepository.class);
    private final ProjectRecurringSeriesHandler handler = new ProjectRecurringSeriesHandler(series);

    private final UUID spaceId = UUID.randomUUID();
    private final SpaceMembership caller =
        new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());

    @Test
    void projectsAcrossAMonthBoundary() {
        // The case the month-based projection cannot serve: a window straddling January and February.
        when(series.findActiveBySpaceId(eq(spaceId), any()))
            .thenReturn(List.of(monthlyOn(LocalDate.of(2026, 1, 28))));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 25), LocalDate.of(2026, 2, 5)))
            .extracting(ProjectedOccurrence::date)
            .containsExactly(LocalDate.of(2026, 1, 28));
    }

    @Test
    void projectsEveryOccurrenceOfALongWindow() {
        when(series.findActiveBySpaceId(eq(spaceId), any()))
            .thenReturn(List.of(monthlyOn(LocalDate.of(2026, 1, 15))));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)))
            .extracting(ProjectedOccurrence::date)
            .containsExactly(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15));
    }

    @Test
    void carriesTheSeriesLabelAmountAndType() {
        when(series.findActiveBySpaceId(eq(spaceId), any()))
            .thenReturn(List.of(monthlyOn(LocalDate.of(2026, 1, 15))));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
            .singleElement()
            .extracting(ProjectedOccurrence::label, ProjectedOccurrence::amount, ProjectedOccurrence::type)
            .containsExactly("Loyer", new BigDecimal("850.00"), TransactionType.EXPENSE);
    }

    @Test
    void neverProjectsADateTheSeriesAlreadyTurnedIntoATransaction() {
        // Jan 15 to Mar 15 are real transactions by now, and the calendar lists them as such.
        // Projecting them as well showed every past occurrence, and today's, twice.
        RecurringTransactionSeries rent = materializedUpTo(monthlyOn(LocalDate.of(2026, 1, 15)), LocalDate.of(2026, 3, 15));
        when(series.findActiveBySpaceId(eq(spaceId), any())).thenReturn(List.of(rent));

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30)))
            .extracting(ProjectedOccurrence::date)
            .containsExactly(LocalDate.of(2026, 4, 15));
    }

    @Test
    void returnsNothingWhenTheSpaceHasNoActiveSeries() {
        when(series.findActiveBySpaceId(eq(spaceId), any())).thenReturn(List.of());

        assertThat(handler.project(caller, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31))).isEmpty();
    }

    private RecurringTransactionSeries materializedUpTo(RecurringTransactionSeries s, LocalDate lastMaterialized) {
        return new RecurringTransactionSeries(s.id(), s.spaceId(), s.label(), s.amount(), s.type(), s.categoryId(),
            s.payerId(), s.contributors(), s.intervalType(), s.intervalCount(), s.anchorDate(), s.endDate(), lastMaterialized);
    }

    private RecurringTransactionSeries monthlyOn(LocalDate anchor) {
        return new RecurringTransactionSeries(
            UUID.randomUUID(), spaceId, "Loyer", new BigDecimal("850.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, anchor, null, null);
    }
}
