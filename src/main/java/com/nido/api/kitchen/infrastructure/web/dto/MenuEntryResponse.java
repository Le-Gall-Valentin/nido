package com.nido.api.kitchen.infrastructure.web.dto;

import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.RecipeCategory;

import java.time.LocalDate;
import java.util.UUID;

public record MenuEntryResponse(
    UUID id, LocalDate date, UUID recipeId, String recipeName, RecipeCategory recipeCategory, int portions, int position
) {
    public static MenuEntryResponse from(MenuEntryView v) {
        return new MenuEntryResponse(v.entry().id(), v.entry().date(), v.recipe().id(), v.recipe().name(),
            v.recipe().category(), v.entry().portions(), v.entry().position());
    }
}
