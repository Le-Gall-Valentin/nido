package com.nido.api.finance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record SetBudgetCommand(UUID spaceId, UUID categoryId, BigDecimal monthlyLimit) {
    public SetBudgetCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(monthlyLimit, "monthlyLimit");
    }
}
