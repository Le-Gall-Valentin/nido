package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsContribution;
import com.nido.api.finance.domain.model.SavingsGoal;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationService
public class ListSavingsGoalsHandler implements ListSavingsGoalsUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public ListSavingsGoalsHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    public List<SavingsGoalDetail> list(SpaceMembership caller) {
        List<SavingsGoal> goals = savingsGoalRepository.findBySpaceId(caller.spaceId());
        Map<UUID, List<SavingsContribution>> contributionsByGoalId = savingsGoalRepository.findContributionsByGoalIds(
            caller.spaceId(), goals.stream().map(SavingsGoal::id).toList());
        return goals.stream()
            .map(g -> new SavingsGoalDetail(g, contributionsByGoalId.getOrDefault(g.id(), List.of())))
            .toList();
    }
}
