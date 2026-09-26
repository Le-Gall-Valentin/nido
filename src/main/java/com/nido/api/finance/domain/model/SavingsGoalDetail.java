package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** A savings goal together with every contribution made toward it — what the frontend needs to render progress. */
public record SavingsGoalDetail(SavingsGoal goal, List<SavingsContribution> contributions) {
    public SavingsGoalDetail {
        Objects.requireNonNull(goal, "goal");
        Objects.requireNonNull(contributions, "contributions");
    }

    /** Everything put toward the goal so far; zero before the first contribution. */
    public BigDecimal totalContributed() {
        return contributions.stream()
            .map(SavingsContribution::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
