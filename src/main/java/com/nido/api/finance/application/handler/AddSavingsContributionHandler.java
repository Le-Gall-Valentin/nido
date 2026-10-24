package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.AddSavingsContributionUseCase;
import com.nido.api.finance.application.service.SavingsNotifier;
import com.nido.api.finance.application.service.SpaceMemberValidator;
import com.nido.api.finance.domain.model.AddSavingsContributionCommand;
import com.nido.api.finance.domain.model.FinanceException;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

@ApplicationService
public class AddSavingsContributionHandler implements AddSavingsContributionUseCase {

    private final SavingsGoalRepository savingsGoalRepository;
    private final SpaceMemberValidator spaceMemberValidator;
    private final SavingsNotifier notifier;

    public AddSavingsContributionHandler(SavingsGoalRepository savingsGoalRepository, SpaceMemberValidator spaceMemberValidator,
                                         SavingsNotifier notifier) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.spaceMemberValidator = spaceMemberValidator;
        this.notifier = notifier;
    }

    @Override
    @Transactional
    public SavingsContribution add(AddSavingsContributionCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        spaceMemberValidator.ensureMember(command.spaceId(), command.memberId());
        // Serializes concurrent contributions to this goal — without it, two requests could
        // both read the same already-contributed total and each add one that, together,
        // push past the target. See RecurringTransactionSeriesRepository.lockForMaterialization
        // for the identical pattern.
        savingsGoalRepository.lockForContribution(command.goalId());
        SavingsGoal goal = savingsGoalRepository.findById(command.goalId()).orElseThrow(FinanceException.SavingsGoalNotFound::new);
        if (!goal.spaceId().equals(command.spaceId())) {
            throw new FinanceException.SavingsGoalNotFound();
        }
        List<SavingsContribution> earlier = savingsGoalRepository.findContributionsByGoalId(goal.id());
        BigDecimal alreadyContributed = earlier.stream()
            .map(SavingsContribution::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (alreadyContributed.add(command.amount()).compareTo(goal.targetAmount()) > 0) {
            throw new FinanceException.ContributionExceedsGoalTarget();
        }
        SavingsContribution created = savingsGoalRepository.addContribution(command);
        notifier.deposited(caller, goal, created, Stream.concat(earlier.stream(), Stream.of(created)).toList());
        return created;
    }
}
