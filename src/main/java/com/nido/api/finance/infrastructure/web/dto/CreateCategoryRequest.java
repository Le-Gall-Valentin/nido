package com.nido.api.finance.infrastructure.web.dto;

import com.nido.api.finance.domain.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 60) String label,
    @NotBlank @Pattern(regexp = "^#[0-9a-f]{6}$") String color,
    @NotBlank @Size(max = 40) String icon,
    @NotNull TransactionType type
) {}
