package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record SavingsGoal(UUID id, UUID spaceId, String name, BigDecimal targetAmount, LocalDate targetDate, String color, String glyph) {
    public SavingsGoal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(targetAmount, "targetAmount");
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(glyph, "glyph");
    }

    /** Whether its members see it differently than {@code other}: its name, target or date — not its colour or icon. */
    public boolean differsForMembersFrom(SavingsGoal other) {
        return !name.equals(other.name)
            || targetAmount.compareTo(other.targetAmount) != 0
            || !Objects.equals(targetDate, other.targetDate);
    }
}
