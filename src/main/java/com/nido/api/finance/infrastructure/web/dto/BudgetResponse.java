package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(UUID id, UUID categoryId, BigDecimal monthlyLimit) {
    public static BudgetResponse from(Budget b) {
        return new BudgetResponse(b.id(), b.categoryId(), b.monthlyLimit());
    }
}
