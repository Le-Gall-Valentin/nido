package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.UpdateSavingsGoalUseCase;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.model.UpdateSavingsGoalCommand;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateSavingsGoalHandler implements UpdateSavingsGoalUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public UpdateSavingsGoalHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    @Transactional
    public SavingsGoalDetail update(UpdateSavingsGoalCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        SavingsGoal existing = savingsGoalRepository.findById(command.goalId()).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        if (!existing.spaceId().equals(command.spaceId())) {
            throw new FinanceException.SavingsGoalNotFound();
        }
        SavingsGoal updated = savingsGoalRepository.update(command);
        return new SavingsGoalDetail(updated, savingsGoalRepository.findContributionsByGoalId(updated.id()));
    }
}
