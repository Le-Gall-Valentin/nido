package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecipeResponse(
    UUID id, String name, String description, RecipeCategory category, int minutes, int referencePortions, boolean favorite,
    List<RecipeIngredientResponse> ingredients, List<String> steps, String note, Instant createdAt, Instant updatedAt
) {
    public static RecipeResponse from(Recipe r) {
        return new RecipeResponse(r.id(), r.name(), r.description(), r.category(), r.minutes(), r.referencePortions(), r.favorite(),
            r.ingredients().stream().map(RecipeIngredientResponse::from).toList(), r.steps(), r.note(),
            r.createdAt(), r.updatedAt());
    }
}
