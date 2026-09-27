package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.finance.application.port.in.MaterializeDueRecurringTransactionsUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.tasks.application.port.in.MaterializeDueRecurringTasksUseCase;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PreparationsTest {

    private final SpaceMembership caller = new SpaceMembership(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.VIEWER, Instant.now());

    @Test
    void theTasksPreparationMaterializesTheDueRecurringTasks() {
        MaterializeDueRecurringTasksUseCase materialize = mock(MaterializeDueRecurringTasksUseCase.class);

        new RecurringTasksPreparation(materialize).prepare(caller);

        verify(materialize).materialize(caller);
    }

    @Test
    void theFinancePreparationMaterializesTheDueRecurringTransactions() {
        MaterializeDueRecurringTransactionsUseCase materialize = mock(MaterializeDueRecurringTransactionsUseCase.class);

        new RecurringTransactionsPreparation(materialize).prepare(caller);

        verify(materialize).materialize(caller);
    }
}
