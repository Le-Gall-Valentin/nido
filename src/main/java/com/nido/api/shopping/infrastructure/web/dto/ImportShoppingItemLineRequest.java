package com.nido.api.shopping.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ImportShoppingItemLineRequest(
    @NotBlank @Size(max = 120) String name, @Size(max = 40) String quantityLabel, @NotNull UUID categoryId
) {}
