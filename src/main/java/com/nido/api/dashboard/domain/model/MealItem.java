package com.nido.api.dashboard.domain.model;

import java.util.UUID;

/** {@code recipeId}, {@code recipeName}, {@code category} and {@code minutes} are null once the recipe is deleted. */
public record MealItem(UUID entryId, UUID recipeId, String recipeName, Category category, Integer minutes, int portions) {

    /** Spelled as the kitchen module spells its recipe categories; the client reads those names. */
    public enum Category { PLAT, EXPRESS, VEGETARIAN, DESSERT, SOUP }
}
