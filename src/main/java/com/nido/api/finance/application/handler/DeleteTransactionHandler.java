package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteTransactionUseCase;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteTransactionHandler implements DeleteTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public DeleteTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public void delete(UUID transactionId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Transaction existing = transactionRepository.findById(transactionId).orElseThrow(FinanceException.TransactionNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new FinanceException.TransactionNotFound();
        }
        // A transaction materialized by a recurring series is only ever removed by deleting
        // the series itself (Task 13's DeleteRecurringSeriesHandler), which cascades. See
        // DeleteTaskHandler for the same one-off-vs-recurring split.
        if (existing.recurringSeriesId() != null) {
            throw new FinanceException.TransactionLinkedToSeries();
        }
        transactionRepository.delete(transactionId);
    }
}
