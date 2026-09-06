package com.nido.api.finance.domain.port.out;

import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {
    List<Budget> findBySpaceId(UUID spaceId);
    Optional<Budget> findByCategoryId(UUID categoryId);
    Budget upsert(SetBudgetCommand command);
    void deleteByCategoryId(UUID categoryId);
}
