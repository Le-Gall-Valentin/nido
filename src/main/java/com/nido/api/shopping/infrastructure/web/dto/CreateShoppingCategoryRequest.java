package com.nido.api.shopping.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShoppingCategoryRequest(@NotBlank @Size(max = 60) String name) {}
