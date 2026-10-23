package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteRecurringSeriesUseCase;
import com.nido.api.finance.application.service.OperationNotifier;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.RecurringTransactionSeries;
import com.nido.api.finance.domain.port.out.RecurringTransactionSeriesRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteRecurringSeriesHandler implements DeleteRecurringSeriesUseCase {

    private final RecurringTransactionSeriesRepository seriesRepository;
    private final OperationNotifier notifier;

    public DeleteRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository, OperationNotifier notifier) {
        this.seriesRepository = seriesRepository;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public void delete(UUID seriesId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        // Same lock RecurringTransactionMaterializer takes: without it, a concurrent lazy
        // materialization could read this series and insert an occurrence for it in the
        // window between our findById and our delete.
        seriesRepository.lockForMaterialization(spaceId);
        RecurringTransactionSeries existing = seriesRepository.findById(seriesId).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new FinanceException.RecurringSeriesNotFound();
        }
        // Detaches (doesn't delete) every finance_transactions row this series already
        // materialized — fk_finance_transactions_recurring_series is ON DELETE SET NULL, so
        // past occurrences stay in history as ordinary one-off transactions once the series
        // that created them is gone; only future occurrences stop.
        seriesRepository.delete(seriesId);
        notifier.recurringStopped(caller, existing);
    }
}
