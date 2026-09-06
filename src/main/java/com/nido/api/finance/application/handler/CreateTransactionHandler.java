package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateTransactionUseCase;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.CreateTransactionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class CreateTransactionHandler implements CreateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SpaceMemberValidator spaceMemberValidator;

    public CreateTransactionHandler(
            TransactionRepository transactionRepository, CategoryRepository categoryRepository, SpaceMemberValidator spaceMemberValidator) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.spaceMemberValidator = spaceMemberValidator;
    }

    @Override
    @Transactional
    public Transaction create(CreateTransactionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        categoryRepository.findById(command.categoryId())
            .filter(category -> category.spaceId().equals(command.spaceId()))
            .orElseThrow(FinanceException.CategoryNotFound::new);
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        if (!resolved.isEmpty() && command.payerId() == null) {
            throw new FinanceException.PayerRequired();
        }
        if (command.payerId() != null) {
            spaceMemberValidator.ensureMember(command.spaceId(), command.payerId());
        }
        resolved.forEach(c -> spaceMemberValidator.ensureMember(command.spaceId(), c.memberId()));
        return transactionRepository.create(command, resolved);
    }
}
