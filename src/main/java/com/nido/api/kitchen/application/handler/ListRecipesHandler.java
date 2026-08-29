package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.ListRecipesUseCase;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeSummaryView;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationService
public class ListRecipesHandler implements ListRecipesUseCase {

    private final RecipeRepository recipeRepository;
    private final MenuRepository menuRepository;

    public ListRecipesHandler(RecipeRepository recipeRepository, MenuRepository menuRepository) {
        this.recipeRepository = recipeRepository;
        this.menuRepository = menuRepository;
    }

    @Override
    public List<RecipeSummaryView> list(SpaceMembership caller) {
        List<Recipe> recipes = recipeRepository.findBySpaceId(caller.spaceId());
        Map<UUID, LocalDate> lastPlanned = menuRepository.lastPlannedOnBySpace(caller.spaceId());
        return recipes.stream()
            .map(r -> new RecipeSummaryView(r, lastPlanned.get(r.id())))
            .toList();
    }
}
