package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteCategoryUseCase;
import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.domain.port.out.CategoryRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteCategoryHandler implements DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final BudgetRepository budgetRepository;

    public DeleteCategoryHandler(CategoryRepository categoryRepository, BudgetRepository budgetRepository) {
        this.categoryRepository = categoryRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    @Transactional
    public void delete(UUID categoryId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        Category existing = categoryRepository.findById(categoryId).orElseThrow(FinanceException.CategoryNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new FinanceException.CategoryNotFound();
        }
        if (categoryRepository.isReferencedByTransactions(categoryId)) {
            throw new FinanceException.CategoryInUse();
        }
        budgetRepository.deleteByCategoryId(categoryId);
        categoryRepository.delete(categoryId);
    }
}
