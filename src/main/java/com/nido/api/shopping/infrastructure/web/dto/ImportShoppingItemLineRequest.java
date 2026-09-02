package com.nido.api.shopping.infrastructure.web.dto;

import com.nido.api.shared.model.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ImportShoppingItemLineRequest(
    @NotBlank @Size(max = 120) String name,
    @DecimalMin(value = "0", inclusive = false) BigDecimal quantity, MeasurementUnit unit,
    @NotNull UUID categoryId
) {}
