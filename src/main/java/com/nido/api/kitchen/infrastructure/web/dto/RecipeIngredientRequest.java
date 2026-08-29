package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.MeasurementUnit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecipeIngredientRequest(
    @Schema(example = "Pâtes") @NotBlank @Size(max = 120) String name,
    @Schema(example = "500") @NotNull @DecimalMin("0.001") BigDecimal quantity,
    @Schema(example = "GRAM") @NotNull MeasurementUnit unit
) {}
