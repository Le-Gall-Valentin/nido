package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.DeleteSavingsGoalUseCase;
import com.nido.api.finance.application.service.SavingsNotifier;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationService
public class DeleteSavingsGoalHandler implements DeleteSavingsGoalUseCase {

    private final SavingsGoalRepository savingsGoalRepository;
    private final SavingsNotifier notifier;

    public DeleteSavingsGoalHandler(SavingsGoalRepository savingsGoalRepository, SavingsNotifier notifier) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.notifier = notifier;
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
        // Read first: the deposits go with the goal, and the mail says who had saved what.
        List<SavingsContribution> contributions = savingsGoalRepository.findContributionsByGoalId(goalId);
        savingsGoalRepository.delete(goalId);
        notifier.goalDeleted(caller, existing, contributions);
    }
}
