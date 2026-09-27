package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.port.out.DashboardPreparation;
import com.nido.api.finance.application.port.in.MaterializeDueRecurringTransactionsUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.stereotype.Component;

/**
 * Turns the recurring transactions that have fallen due into real rows before the finance source
 * reads, so the month's figures and the upcoming list never count an occurrence twice or miss one.
 */
@Component
public class RecurringTransactionsPreparation implements DashboardPreparation {

    private final MaterializeDueRecurringTransactionsUseCase materialize;

    public RecurringTransactionsPreparation(MaterializeDueRecurringTransactionsUseCase materialize) {
        this.materialize = materialize;
    }

    @Override
    public void prepare(SpaceMembership caller) {
        materialize.materialize(caller);
    }
}
