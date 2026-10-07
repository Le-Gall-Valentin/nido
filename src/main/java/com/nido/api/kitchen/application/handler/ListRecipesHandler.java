package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.ListRecipesUseCase;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeSummaryView;
import com.nido.api.kitchen.domain.port.out.MenuRepository;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.shared.model.NameOrdering;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
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
    @Transactional(readOnly = true)
    public List<RecipeSummaryView> list(SpaceMembership caller) {
        List<Recipe> recipes = recipeRepository.findBySpaceId(caller.spaceId());
        Map<UUID, LocalDate> lastPlanned = menuRepository.lastPlannedOnBySpace(caller.spaceId());
        // Sorted here rather than in SQL, the names being sealed.
        return recipes.stream()
            .sorted(Comparator.comparing(Recipe::name, NameOrdering.comparator()))
            .map(r -> new RecipeSummaryView(r, lastPlanned.get(r.id())))
            .toList();
    }
}
