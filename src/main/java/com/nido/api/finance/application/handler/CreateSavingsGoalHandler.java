package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.CreateSavingsGoalUseCase;
import com.nido.api.finance.domain.model.CreateSavingsGoalCommand;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateSavingsGoalHandler implements CreateSavingsGoalUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public CreateSavingsGoalHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    @Transactional
    public SavingsGoal create(CreateSavingsGoalCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        return savingsGoalRepository.create(command);
    }
}
