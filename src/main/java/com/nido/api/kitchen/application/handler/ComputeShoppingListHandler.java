package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.ComputeShoppingListUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.ShoppingListAggregator;
import com.nido.api.kitchen.domain.model.ShoppingListLine;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationService
public class ComputeShoppingListHandler implements ComputeShoppingListUseCase {

    private final MenuRepository menuRepository;
    private final RecipeRepository recipeRepository;
    private final ShoppingListAggregator aggregator = new ShoppingListAggregator();

    public ComputeShoppingListHandler(MenuRepository menuRepository, RecipeRepository recipeRepository) {
        this.menuRepository = menuRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<ShoppingListLine> compute(SpaceMembership caller, LocalDate from, LocalDate to) {
        // findBySpaceIdAndDateRange already orders by (date, position), which is exactly the
        // chronological order the aggregator needs to pick a deterministic display name.
        List<MenuEntry> entries = menuRepository.findBySpaceIdAndDateRange(caller.spaceId(), from, to);
        if (entries.isEmpty()) {
            return List.of();
        }
        List<UUID> recipeIds = entries.stream().map(MenuEntry::recipeId).distinct().toList();
        Map<UUID, Recipe> recipesById = recipeRepository.findByIds(recipeIds).stream()
            .collect(Collectors.toMap(Recipe::id, r -> r));
        List<RecipeIngredient> scaledInOrder = entries.stream()
            .flatMap(entry -> recipesById.get(entry.recipeId()).scaledIngredients(entry.portions()).stream())
            .toList();
        return aggregator.aggregate(scaledInOrder);
    }
}
