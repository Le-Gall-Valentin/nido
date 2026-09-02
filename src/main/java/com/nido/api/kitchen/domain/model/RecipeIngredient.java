package com.nido.api.kitchen.domain.model;

import com.nido.api.shared.model.MeasurementUnit;

import java.math.BigDecimal;
import java.util.Objects;

public record RecipeIngredient(String name, BigDecimal quantity, MeasurementUnit unit) {
    public RecipeIngredient {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(unit, "unit");
    }
}
