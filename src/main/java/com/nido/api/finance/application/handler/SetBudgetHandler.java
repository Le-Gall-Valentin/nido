package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.SetBudgetUseCase;
import com.nido.api.finance.domain.model.Budget;
import com.nido.api.finance.domain.model.SetBudgetCommand;
import com.nido.api.finance.domain.port.out.BudgetRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class SetBudgetHandler implements SetBudgetUseCase {

    private final BudgetRepository budgetRepository;

    public SetBudgetHandler(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Override
    @Transactional
    public Budget set(SetBudgetCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        return budgetRepository.upsert(command);
    }
}
