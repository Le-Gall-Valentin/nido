package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.space.domain.model.SpaceMembership;

public interface CreateRecipeUseCase {
    Recipe create(CreateRecipeCommand command, SpaceMembership caller);
}
