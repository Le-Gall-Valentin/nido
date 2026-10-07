package com.nido.api.finance.infrastructure.persistence.adapter;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.finance.infrastructure.persistence.entity.FinanceBudgetEntity;
import com.nido.api.finance.infrastructure.persistence.repository.FinanceBudgetJpaRepository;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
        return budgets.findBySpaceId(spaceId).stream().map(e -> toDomain(e, spaceId)).toList();
    }

    @Override
    public Optional<Budget> findByCategoryId(UUID categoryId) {
        return budgets.findByCategoryId(categoryId).map(e -> toDomain(e, e.getSpaceId()));
    }

    @Override
    @Transactional
    public Budget upsert(SetBudgetCommand command) {
        FinanceBudgetEntity e = budgets.findByCategoryId(command.categoryId()).orElseGet(FinanceBudgetEntity::new);
        e.setSpaceId(command.spaceId());
        e.setCategoryId(command.categoryId());
        e.setMonthlyLimitEncrypted(sealers.forSpace(command.spaceId()).seal(FinanceBudgetEntity.MONTHLY_LIMIT, e.getId(),
            command.monthlyLimit().toPlainString()));
        FinanceBudgetEntity saved = budgets.saveAndFlush(e);
        return toDomain(saved, command.spaceId());
    }

    @Override
    @Transactional
    public void deleteByCategoryId(UUID categoryId) {
        budgets.deleteByCategoryId(categoryId);
        budgets.flush();
    }

    private Budget toDomain(FinanceBudgetEntity e, UUID spaceId) {
        String decrypted = sealers.forSpace(spaceId).open(FinanceBudgetEntity.MONTHLY_LIMIT, e.getId(), e.getMonthlyLimitEncrypted());
        return new Budget(e.getId(), e.getSpaceId(), e.getCategoryId(), new BigDecimal(decrypted));
    }
}
