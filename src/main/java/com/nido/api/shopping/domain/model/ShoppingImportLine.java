package com.nido.api.shopping.domain.model;

import com.nido.api.shared.model.MeasurementUnit;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** One suggested line from Kitchen's computed weekly shopping list, category-assigned by the frontend modal. */
public record ShoppingImportLine(String name, BigDecimal quantity, MeasurementUnit unit, UUID categoryId) {
    public ShoppingImportLine {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(categoryId, "categoryId");
    }
}
