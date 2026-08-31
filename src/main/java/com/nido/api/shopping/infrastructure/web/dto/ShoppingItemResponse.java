package com.nido.api.shopping.infrastructure.web.dto;

import com.nido.api.shopping.domain.model.ShoppingItem;

import java.util.UUID;

public record ShoppingItemResponse(UUID id, UUID categoryId, String name, String quantityLabel, boolean done, int position) {
    public static ShoppingItemResponse from(ShoppingItem i) {
        return new ShoppingItemResponse(i.id(), i.categoryId(), i.name(), i.quantityLabel(), i.done(), i.position());
    }
}
