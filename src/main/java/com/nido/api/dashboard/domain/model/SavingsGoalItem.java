package com.nido.api.dashboard.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** {@code monthlyNeeded} is null without a target date and once the goal is reached. */
public record SavingsGoalItem(UUID goalId, String name, String glyph, String color, BigDecimal target,
                              BigDecimal contributed, LocalDate targetDate, GoalState state, BigDecimal monthlyNeeded) {
}
