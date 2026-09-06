package com.nido.api.finance.application.handler;

import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurrenceInterval;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.space.domain.model.SpaceException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteRecurringSeriesHandlerTest {

    @Mock RecurringTransactionSeriesRepository seriesRepository;
    private DeleteRecurringSeriesHandler handler;
    private final UUID spaceId = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new DeleteRecurringSeriesHandler(seriesRepository);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private RecurringTransactionSeries series(UUID spaceId) {
        return new RecurringTransactionSeries(seriesId, spaceId, "Loyer", new BigDecimal("800.00"), TransactionType.EXPENSE,
            UUID.randomUUID(), null, List.of(), RecurrenceInterval.MONTHLY, 1, LocalDate.of(2026, 1, 1), null, null);
    }

    @Test
    void a_member_can_delete_a_recurring_series() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series(spaceId)));

        handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER));

        verify(seriesRepository).delete(seriesId);
    }

    @Test
    void deleting_a_series_takes_the_materialization_lock_to_avoid_racing_a_concurrent_lazy_materialization() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series(spaceId)));

        handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER));

        verify(seriesRepository).lockForMaterialization(spaceId);
    }

    @Test
    void deleting_a_series_belonging_to_another_space_is_rejected() {
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series(UUID.randomUUID())));

        assertThatThrownBy(() -> handler.delete(seriesId, spaceId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(FinanceException.RecurringSeriesNotFound.class);
    }

    @Test
    void a_viewer_cannot_delete_a_recurring_series() {
        assertThatThrownBy(() -> handler.delete(seriesId, spaceId, membership(SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }
}
