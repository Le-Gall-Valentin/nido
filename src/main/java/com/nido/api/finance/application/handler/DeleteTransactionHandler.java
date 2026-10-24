package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteTransactionUseCase;
import com.nido.api.finance.application.service.OperationNotifier;
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
    private final OperationNotifier notifier;

    public DeleteTransactionHandler(TransactionRepository transactionRepository, OperationNotifier notifier) {
        this.transactionRepository = transactionRepository;
        this.notifier = notifier;
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
        transactionRepository.delete(transactionId);
        notifier.transactionDeleted(caller, existing);
    }
}
