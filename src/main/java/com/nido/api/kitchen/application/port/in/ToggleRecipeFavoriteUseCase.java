package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.space.domain.model.SpaceMembership;

import java.util.UUID;

public interface ToggleRecipeFavoriteUseCase {
    Recipe toggleFavorite(UUID recipeId, SpaceMembership caller);
}
