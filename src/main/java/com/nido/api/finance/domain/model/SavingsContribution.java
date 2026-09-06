package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record SavingsContribution(UUID id, UUID goalId, UUID memberId, BigDecimal amount, LocalDate date) {
    public SavingsContribution {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(goalId, "goalId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(date, "date");
    }
}
