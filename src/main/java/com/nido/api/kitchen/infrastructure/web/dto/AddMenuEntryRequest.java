package com.nido.api.kitchen.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AddMenuEntryRequest(
    @Schema(example = "2026-09-07") @NotNull LocalDate date,
    @NotNull UUID recipeId,
    @Min(1) int portions
) {}
