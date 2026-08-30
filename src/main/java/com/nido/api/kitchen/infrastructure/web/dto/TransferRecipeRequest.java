package com.nido.api.kitchen.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Destination d'un déplacement ou d'une copie de recette")
public record TransferRecipeRequest(
    @NotNull UUID destinationSpaceId
) {}
