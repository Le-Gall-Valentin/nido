package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.BudgetLine;
import com.nido.api.finance.domain.model.BudgetStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetLineResponse(UUID categoryId, BigDecimal monthlyLimit, BigDecimal spent, BudgetStatus status) {
    public static BudgetLineResponse from(BudgetLine b) {
        return new BudgetLineResponse(b.categoryId(), b.monthlyLimit(), b.spent(), b.status());
    }
}
