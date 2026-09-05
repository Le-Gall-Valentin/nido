package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteRecurringSeriesUseCase;
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

    public DeleteRecurringSeriesHandler(RecurringTransactionSeriesRepository seriesRepository) {
        this.seriesRepository = seriesRepository;
    }

    @Override
    @Transactional
    public void delete(UUID seriesId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        RecurringTransactionSeries existing = seriesRepository.findById(seriesId).orElseThrow(FinanceException.RecurringSeriesNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new FinanceException.RecurringSeriesNotFound();
        }
        // Cascades to every finance_transactions row this series materialized (fk_finance_transactions_recurring_series, ON DELETE CASCADE).
        seriesRepository.delete(seriesId);
    }
}
