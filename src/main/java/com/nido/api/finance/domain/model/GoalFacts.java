package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** What a mail tells about a savings goal: what its members see of it, not its colour or its icon. */
public record GoalFacts(String name, BigDecimal target, LocalDate targetDate) {

    public static GoalFacts of(SavingsGoal goal) {
        return new GoalFacts(goal.name(), goal.targetAmount(), goal.targetDate());
    }
}
