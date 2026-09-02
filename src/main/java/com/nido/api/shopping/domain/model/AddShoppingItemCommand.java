package com.nido.api.shopping.domain.model;

import com.nido.api.shared.model.MeasurementUnit;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record AddShoppingItemCommand(UUID spaceId, UUID categoryId, String name, BigDecimal quantity, MeasurementUnit unit) {
    public AddShoppingItemCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
    }
}
