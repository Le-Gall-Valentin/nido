package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.RecipeIngredient;

import java.math.BigDecimal;

public record RecipeIngredientResponse(String name, BigDecimal quantity, MeasurementUnit unit) {
    public static RecipeIngredientResponse from(RecipeIngredient i) {
        return new RecipeIngredientResponse(i.name(), i.quantity(), i.unit());
    }
}
