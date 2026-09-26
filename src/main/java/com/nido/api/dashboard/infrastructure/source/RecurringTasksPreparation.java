package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.port.out.DashboardPreparation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.tasks.application.port.in.MaterializeDueRecurringTasksUseCase;
import org.springframework.stereotype.Component;

/**
 * Turns the recurring tasks that have fallen due into real rows before any source reads, so that
 * today's rotating chore carries the member whose turn it is and can be ticked from the dashboard.
 * Runs in the tasks module's own write transaction — never inside a read-only one.
 */
@Component
public class RecurringTasksPreparation implements DashboardPreparation {

    private final MaterializeDueRecurringTasksUseCase materialize;

    public RecurringTasksPreparation(MaterializeDueRecurringTasksUseCase materialize) {
        this.materialize = materialize;
    }

    @Override
    public void prepare(SpaceMembership caller) {
        materialize.materialize(caller);
    }
}
