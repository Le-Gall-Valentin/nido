package com.nido.api.kitchen.application.port.in;

import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface DeleteRecipeUseCase {
    void delete(UUID recipeId, SpaceMembership caller);
}
