package com.nido.api.finance.application.handler;

import com.nido.api.finance.application.port.in.ListSavingsGoalsUseCase;
import com.nido.api.finance.domain.model.SavingsGoalDetail;
import com.nido.api.finance.domain.port.out.SavingsGoalRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.List;

@ApplicationService
public class ListSavingsGoalsHandler implements ListSavingsGoalsUseCase {

    private final SavingsGoalRepository savingsGoalRepository;

    public ListSavingsGoalsHandler(SavingsGoalRepository savingsGoalRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
    }

    @Override
    public List<SavingsGoalDetail> list(SpaceMembership caller) {
        return savingsGoalRepository.findBySpaceId(caller.spaceId()).stream()
            .map(g -> new SavingsGoalDetail(g, savingsGoalRepository.findContributionsByGoalId(g.id())))
            .toList();
    }
}
