package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateTransactionUseCase;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class CreateTransactionHandler implements CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;

    public CreateTransactionHandler(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public Transaction create(CreateTransactionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        return transactionRepository.create(command, resolved);
    }
}
