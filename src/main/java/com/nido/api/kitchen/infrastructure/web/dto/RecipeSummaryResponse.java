package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeSummaryView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RecipeSummaryResponse(
    UUID id, String name, String description, RecipeCategory category, int minutes, int referencePortions, boolean favorite,
    List<RecipeIngredientResponse> ingredients, List<String> steps, String note, LocalDate lastPlannedOn
) {
    public static RecipeSummaryResponse from(RecipeSummaryView v) {
        Recipe r = v.recipe();
        return new RecipeSummaryResponse(r.id(), r.name(), r.description(), r.category(), r.minutes(), r.referencePortions(), r.favorite(),
            r.ingredients().stream().map(RecipeIngredientResponse::from).toList(), r.steps(), r.note(), v.lastPlannedOn());
    }
}
