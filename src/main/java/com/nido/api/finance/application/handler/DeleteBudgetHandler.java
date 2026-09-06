package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteBudgetUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteBudgetHandler implements DeleteBudgetUseCase {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public DeleteBudgetHandler(BudgetRepository budgetRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void delete(UUID spaceId, UUID categoryId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Category category = categoryRepository.findById(categoryId).orElseThrow(FinanceException.CategoryNotFound::new);
        if (!category.spaceId().equals(spaceId)) {
            throw new FinanceException.CategoryNotFound();
        }
        budgetRepository.deleteByCategoryId(categoryId);
    }
}
