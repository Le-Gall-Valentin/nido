package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record AddSavingsContributionCommand(UUID goalId, UUID spaceId, UUID memberId, BigDecimal amount, LocalDate date) {
    public AddSavingsContributionCommand {
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(date, "date");
    }
}
