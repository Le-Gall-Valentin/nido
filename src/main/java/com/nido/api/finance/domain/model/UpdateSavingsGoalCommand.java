package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record UpdateSavingsGoalCommand(UUID goalId, UUID spaceId, String name, BigDecimal targetAmount, LocalDate targetDate, String color, String glyph) {
    public UpdateSavingsGoalCommand {
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(targetAmount, "targetAmount");
        SavingsGoalAppearance.ensureValid(color, glyph);
    }
}
