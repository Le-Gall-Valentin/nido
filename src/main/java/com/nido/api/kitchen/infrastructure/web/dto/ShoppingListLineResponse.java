package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.ShoppingListLine;

import java.math.BigDecimal;

public record ShoppingListLineResponse(String name, BigDecimal quantity, MeasurementUnit unit) {
    public static ShoppingListLineResponse from(ShoppingListLine line) {
        return new ShoppingListLineResponse(line.name(), line.quantity(), line.unit());
    }
}
