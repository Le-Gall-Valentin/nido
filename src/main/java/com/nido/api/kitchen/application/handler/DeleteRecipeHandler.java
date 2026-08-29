package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.DeleteRecipeUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class DeleteRecipeHandler implements DeleteRecipeUseCase {

    private final RecipeRepository recipeRepository;

    public DeleteRecipeHandler(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public void delete(UUID recipeId, SpaceMembership caller) {
        Recipe existing = recipeRepository.findById(recipeId).orElseThrow(KitchenException.RecipeNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        caller.ensureCanWrite();
        recipeRepository.delete(recipeId);
    }
}
