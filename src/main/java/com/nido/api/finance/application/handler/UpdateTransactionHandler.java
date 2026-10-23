package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateTransactionUseCase;
import com.nido.api.finance.application.service.BudgetWatch;
import com.nido.api.finance.application.service.OperationNotifier;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.Contribution;
import com.nido.api.finance.domain.model.ContributionSplitter;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.Transaction;
import com.nido.api.finance.domain.model.UpdateTransactionCommand;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.finance.domain.port.out.TransactionRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationService
public class UpdateTransactionHandler implements UpdateTransactionUseCase {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final SpaceMemberValidator spaceMemberValidator;
    private final OperationNotifier notifier;
    private final BudgetWatch budgetWatch;

    public UpdateTransactionHandler(
            TransactionRepository transactionRepository, CategoryRepository categoryRepository, SpaceMemberValidator spaceMemberValidator,
            OperationNotifier notifier, BudgetWatch budgetWatch) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.spaceMemberValidator = spaceMemberValidator;
        this.notifier = notifier;
        this.budgetWatch = budgetWatch;
    }

    @Override
    @Transactional
    public Transaction update(UpdateTransactionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Category category = categoryRepository.findById(command.categoryId())
            .filter(c -> c.spaceId().equals(command.spaceId()))
            .orElseThrow(FinanceException.CategoryNotFound::new);
        if (category.type() != command.type()) {
            throw new FinanceException.CategoryTypeMismatch();
        }
        List<Contribution> resolved = ContributionSplitter.resolve(command.amount(), command.contributors());
        if (!resolved.isEmpty() && command.payerId() == null) {
            throw new FinanceException.PayerRequired();
        }
        if (command.payerId() != null) {
            spaceMemberValidator.ensureMember(command.spaceId(), command.payerId());
        }
        resolved.forEach(c -> spaceMemberValidator.ensureMember(command.spaceId(), c.memberId()));
        Transaction existing = transactionRepository.findById(command.transactionId()).orElseThrow(FinanceException.TransactionNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new FinanceException.TransactionNotFound();
        }
        // Watched as the operation will be: moved to another category, only the new one can go up.
        Optional<BudgetWatch.Watched> watched =
            budgetWatch.before(command.spaceId(), command.type(), command.categoryId(), command.date());
        Transaction updated = transactionRepository.update(command, resolved);
        notifier.transactionUpdated(caller, existing, updated);
        budgetWatch.after(caller, watched, updated);
        return updated;
    }
}
