package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.ListMenuEntriesUseCase;
import com.nido.api.kitchen.domain.model.MenuEntry;
import com.nido.api.kitchen.domain.model.MenuEntryView;
import com.nido.api.kitchen.domain.model.Recipe;
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
public class ListMenuEntriesHandler implements ListMenuEntriesUseCase {

    private final MenuRepository menuRepository;
    private final RecipeRepository recipeRepository;

    public ListMenuEntriesHandler(MenuRepository menuRepository, RecipeRepository recipeRepository) {
        this.menuRepository = menuRepository;
        this.recipeRepository = recipeRepository;
    }

    @Override
    public List<MenuEntryView> list(SpaceMembership caller, LocalDate from, LocalDate to) {
        List<MenuEntry> entries = menuRepository.findBySpaceIdAndDateRange(caller.spaceId(), from, to);
        if (entries.isEmpty()) {
            return List.of();
        }
        List<UUID> recipeIds = entries.stream().map(MenuEntry::recipeId).distinct().toList();
        Map<UUID, Recipe> recipesById = recipeRepository.findByIds(recipeIds).stream()
            .collect(Collectors.toMap(Recipe::id, r -> r));
        return entries.stream().map(e -> new MenuEntryView(e, recipesById.get(e.recipeId()))).toList();
    }
}
