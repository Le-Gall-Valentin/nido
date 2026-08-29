package com.nido.api.kitchen.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Recipe(
    UUID id,
    UUID spaceId,
    String name,
    RecipeCategory category,
    int minutes,
    int referencePortions,
    boolean favorite,
    List<RecipeIngredient> ingredients,
    List<String> steps,
    Instant createdAt,
    Instant updatedAt
) {
    public Recipe {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(ingredients, "ingredients");
        Objects.requireNonNull(steps, "steps");
    }

    /** Every ingredient's quantity scaled by targetPortions ÷ referencePortions. */
    public List<RecipeIngredient> scaledIngredients(int targetPortions) {
        BigDecimal factor = BigDecimal.valueOf(targetPortions)
            .divide(BigDecimal.valueOf(referencePortions), 6, RoundingMode.HALF_UP);
        return ingredients.stream()
            .map(i -> new RecipeIngredient(i.name(), i.quantity().multiply(factor), i.unit()))
            .toList();
    }
}
