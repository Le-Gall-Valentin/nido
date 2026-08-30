package com.nido.api.kitchen.application.handler;

import com.nido.api.kitchen.application.port.in.MoveRecipeUseCase;
import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.shared.annotation.ApplicationService;
import com.nido.api.space.application.port.in.ResolveMembershipUseCase;
import com.nido.api.space.domain.model.SpaceMembership;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@ApplicationService
public class MoveRecipeHandler implements MoveRecipeUseCase {

    private final RecipeRepository recipeRepository;
    private final ResolveMembershipUseCase resolveMembershipUseCase;

    public MoveRecipeHandler(RecipeRepository recipeRepository, ResolveMembershipUseCase resolveMembershipUseCase) {
        this.recipeRepository = recipeRepository;
        this.resolveMembershipUseCase = resolveMembershipUseCase;
    }

    @Override
    @Transactional
    public Recipe move(UUID recipeId, UUID destinationSpaceId, SpaceMembership caller) {
        caller.ensureCanWrite();
        if (destinationSpaceId.equals(caller.spaceId())) {
            throw new KitchenException.SameSpaceTransfer();
        }
        Recipe source = recipeRepository.findById(recipeId).orElseThrow(KitchenException.RecipeNotFound::new);
        if (!source.spaceId().equals(caller.spaceId())) {
            throw new KitchenException.RecipeNotFound();
        }
        SpaceMembership destination = resolveMembershipUseCase.resolve(destinationSpaceId, caller.userId());
        destination.ensureCanWrite();
        Recipe moved = recipeRepository.create(new CreateRecipeCommand(
            destinationSpaceId, source.name(), source.description(), source.category(),
            source.minutes(), source.referencePortions(), source.ingredients(), source.steps(), source.note()));
        recipeRepository.delete(recipeId);
        return moved;
    }
}
