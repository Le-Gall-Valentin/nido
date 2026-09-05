package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateTransactionUseCase;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class UpdateTransactionHandler implements UpdateTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public UpdateTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Transaction update(UpdateTransactionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        Transaction existing = transactionRepository.findById(command.transactionId()).orElseThrow(FinanceException.TransactionNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new FinanceException.TransactionNotFound();
        }
        return transactionRepository.update(command, resolved);
    }
}
