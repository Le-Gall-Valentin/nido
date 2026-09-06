package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteSavingsGoalUseCase;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteSavingsGoalHandler implements DeleteSavingsGoalUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public DeleteSavingsGoalHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    @Transactional
    public void delete(UUID goalId, UUID spaceId, SpaceMembership caller) {
        caller.ensureSameSpace(spaceId);
        caller.ensureCanWrite();
        SavingsGoal existing = savingsGoalRepository.findById(goalId).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        if (!existing.spaceId().equals(spaceId)) {
            throw new FinanceException.SavingsGoalNotFound();
        }
        savingsGoalRepository.delete(goalId);
    }
}
