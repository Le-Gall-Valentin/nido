package com.nido.api.kitchen.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record CreateRecipeCommand(
    UUID spaceId,
    String name,
    String description,
    RecipeCategory category,
    int minutes,
    int referencePortions,
    List<RecipeIngredient> ingredients,
    List<String> steps,
    String note
) {
    public CreateRecipeCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(ingredients, "ingredients");
        Objects.requireNonNull(steps, "steps");
    }
}
