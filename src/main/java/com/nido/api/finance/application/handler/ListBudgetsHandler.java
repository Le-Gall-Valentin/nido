package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListBudgetsUseCase;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@ApplicationService
public class ListBudgetsHandler implements ListBudgetsUseCase {

    private final BudgetRepository budgetRepository;

    public ListBudgetsHandler(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Budget> list(SpaceMembership caller) {
        return budgetRepository.findBySpaceId(caller.spaceId());
    }
}
