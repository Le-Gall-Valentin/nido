package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.RecipeCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Création d'une recette")
public record CreateRecipeRequest(
    @Schema(example = "Pâtes bolognaise") @NotBlank @Size(max = 120) String name,
    @Size(max = 2000) String description,
    @NotNull RecipeCategory category,
    @Min(1) int minutes,
    @Min(1) int referencePortions,
    @NotEmpty @Size(max = 100) @Valid List<RecipeIngredientRequest> ingredients,
    @NotNull @Size(max = 100) List<@NotBlank @Size(max = 2000) String> steps,
    @Size(max = 2000) String note
) {}
