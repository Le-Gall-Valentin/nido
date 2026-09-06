package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.Category;
import com.nido.api.finance.domain.model.TransactionType;

import java.util.UUID;

public record CategoryResponse(UUID id, String label, String color, String icon, boolean isDefault, TransactionType type) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.label(), c.color(), c.icon(), c.isDefault(), c.type());
    }
}
