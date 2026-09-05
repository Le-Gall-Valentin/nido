package com.nido.api.finance.domain.model;

import java.util.List;
import java.util.Objects;

/** A savings goal together with every contribution made toward it — what the frontend needs to render progress. */
public record SavingsGoalDetail(SavingsGoal goal, List<SavingsContribution> contributions) {
    public SavingsGoalDetail {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(contributions, "contributions");
    }
}
