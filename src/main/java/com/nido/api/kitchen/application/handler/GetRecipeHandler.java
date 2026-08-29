package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.GetRecipeUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

@ApplicationService
public class GetRecipeHandler implements GetRecipeUseCase {

    private final RecipeRepository recipeRepository;

    public GetRecipeHandler(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    public Recipe get(UUID recipeId, SpaceMembership caller) {
        Recipe recipe = recipeRepository.findById(recipeId).orElseThrow(KitchenException.RecipeNotFound::new);
        if (!recipe.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        return recipe;
    }
}
