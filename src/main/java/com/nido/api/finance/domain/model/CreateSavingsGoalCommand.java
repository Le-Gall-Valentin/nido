package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record CreateSavingsGoalCommand(UUID spaceId, String name, BigDecimal targetAmount, LocalDate targetDate) {
    public CreateSavingsGoalCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(targetAmount, "targetAmount");
    }
}
