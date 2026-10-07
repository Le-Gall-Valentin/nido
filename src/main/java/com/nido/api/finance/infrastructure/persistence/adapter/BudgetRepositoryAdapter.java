package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceBudgetEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceBudgetJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BudgetRepositoryAdapter implements BudgetRepository {

    private final FinanceBudgetJpaRepository budgets;
    private final SpaceSealers sealers;

    public BudgetRepositoryAdapter(FinanceBudgetJpaRepository budgets, SpaceSealers sealers) {
        this.budgets = budgets;
        this.sealers = sealers;
    }

    @Override
    public List<Budget> findBySpaceId(UUID spaceId) {
        return budgets.findBySpaceId(spaceId).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Budget> findByCategoryId(UUID categoryId) {
        return budgets.findByCategoryId(categoryId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Budget upsert(SetBudgetCommand command) {
        FinanceBudgetEntity e = budgets.findByCategoryId(command.categoryId()).orElseGet(() -> {
            FinanceBudgetEntity created = new FinanceBudgetEntity();
            created.setSpaceId(command.spaceId());
            created.setCategoryId(command.categoryId());
            return created;
        });
        // The key of the space the row is stored in, whatever the command says.
        e.setMonthlyLimitEncrypted(SealedAmounts.seal(sealers.forSpace(e.getSpaceId()), FinanceBudgetEntity.MONTHLY_LIMIT, e.getId(),
            command.monthlyLimit()));
        return toDomain(budgets.saveAndFlush(e));
    }

    @Override
    @Transactional
    public void deleteByCategoryId(UUID categoryId) {
        budgets.deleteByCategoryId(categoryId);
        budgets.flush();
    }

    private Budget toDomain(FinanceBudgetEntity e) {
        return new Budget(e.getId(), e.getSpaceId(), e.getCategoryId(),
            SealedAmounts.open(sealers.forSpace(e.getSpaceId()), FinanceBudgetEntity.MONTHLY_LIMIT, e.getId(), e.getMonthlyLimitEncrypted()));
    }
}
