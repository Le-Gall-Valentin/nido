package com.nido.api.shopping.infrastructure.web.dto;

import com.nido.api.shopping.domain.model.ShoppingCategory;

import java.util.UUID;

public record ShoppingCategoryResponse(UUID id, String name, int position, boolean fallback) {
    public static ShoppingCategoryResponse from(ShoppingCategory c) {
        return new ShoppingCategoryResponse(c.id(), c.name(), c.position(), c.fallback());
    }
}
