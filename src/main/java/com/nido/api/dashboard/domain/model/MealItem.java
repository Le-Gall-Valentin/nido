package com.nido.api.dashboard.domain.model;

import java.util.UUID;

/** {@code recipeId}, {@code recipeName}, {@code category} and {@code minutes} are null once the recipe is deleted. */
public record MealItem(UUID entryId, UUID recipeId, String recipeName, String category, Integer minutes, int portions) {
}
