package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.CategoryAmount;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAmountResponse(UUID categoryId, BigDecimal amount) {
    public static CategoryAmountResponse from(CategoryAmount a) {
        return new CategoryAmountResponse(a.categoryId(), a.amount());
    }
}
