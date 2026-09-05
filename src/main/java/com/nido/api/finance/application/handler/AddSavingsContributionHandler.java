package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.AddSavingsContributionUseCase;
import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class AddSavingsContributionHandler implements AddSavingsContributionUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public AddSavingsContributionHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    @Transactional
    public SavingsContribution add(AddSavingsContributionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        SavingsGoal goal = savingsGoalRepository.findById(command.goalId()).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        if (!goal.spaceId().equals(command.spaceId())) {
            throw new FinanceException.SavingsGoalNotFound();
        }
        return savingsGoalRepository.addContribution(command);
    }
}
