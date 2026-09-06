package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SetBudgetUseCase;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.domain.model.TransactionType;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetBudgetHandler implements SetBudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public SetBudgetHandler(BudgetRepository budgetRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public Budget set(SetBudgetCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Category category = categoryRepository.findById(command.categoryId()).orElseThrow(FinanceException.CategoryNotFound::new);
        if (!category.spaceId().equals(command.spaceId())) {
            throw new FinanceException.CategoryNotFound();
        }
        if (category.type() != TransactionType.EXPENSE) {
            throw new FinanceException.BudgetRequiresExpenseCategory();
        }
        return budgetRepository.upsert(command);
    }
}
