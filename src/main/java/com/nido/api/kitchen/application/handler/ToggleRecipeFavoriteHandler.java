package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.ToggleRecipeFavoriteUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class ToggleRecipeFavoriteHandler implements ToggleRecipeFavoriteUseCase {

    private final RecipeRepository recipeRepository;

    public ToggleRecipeFavoriteHandler(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public Recipe toggleFavorite(UUID recipeId, SpaceMembership caller) {
        Recipe existing = recipeRepository.findById(recipeId).orElseThrow(KitchenException.RecipeNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        caller.ensureCanWrite();
        boolean toggled = !existing.favorite();
        recipeRepository.setFavorite(recipeId, toggled);
        return new Recipe(existing.id(), existing.spaceId(), existing.name(), existing.category(),
            existing.minutes(), existing.referencePortions(), toggled, existing.ingredients(),
            existing.steps(), existing.createdAt(), existing.updatedAt());
    }
}
