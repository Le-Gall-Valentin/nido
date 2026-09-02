package com.nido.api.shopping.infrastructure.web.dto;

import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.shopping.domain.model.ShoppingItem;

import java.math.BigDecimal;
import java.util.UUID;

public record ShoppingItemResponse(
    UUID id, UUID categoryId, String name, BigDecimal quantity, MeasurementUnit unit, boolean done, int position
) {
    public static ShoppingItemResponse from(ShoppingItem i) {
        return new ShoppingItemResponse(i.id(), i.categoryId(), i.name(), i.quantity(), i.unit(), i.done(), i.position());
    }
}
