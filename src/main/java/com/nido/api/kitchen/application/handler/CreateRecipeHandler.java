package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.CreateRecipeUseCase;
import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

@ApplicationService
public class CreateRecipeHandler implements CreateRecipeUseCase {

    private final RecipeRepository recipeRepository;

    public CreateRecipeHandler(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Override
    @Transactional
    public Recipe create(CreateRecipeCommand command, SpaceMembership caller) {
        caller.ensureSameSpace(command.spaceId());
        caller.ensureCanWrite();
        return recipeRepository.create(command);
    }
}
