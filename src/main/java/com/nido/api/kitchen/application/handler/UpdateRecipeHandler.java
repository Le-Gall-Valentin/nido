package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.UpdateRecipeUseCase;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class UpdateRecipeHandler implements UpdateRecipeUseCase {

    private final RecipeRepository recipeRepository;

    public UpdateRecipeHandler(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public Recipe update(UpdateRecipeCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        Recipe existing = recipeRepository.findById(command.recipeId())
            .orElseThrow(KitchenException.RecipeNotFound::new);
        if (!existing.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        return recipeRepository.update(command);
    }
}
