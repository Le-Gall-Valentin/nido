package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.BudgetLine;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLineResponse(UUID categoryId, BigDecimal monthlyLimit, BigDecimal spent) {
    public static BudgetLineResponse from(BudgetLine b) {
        return new BudgetLineResponse(b.categoryId(), b.monthlyLimit(), b.spent());
    }
}
