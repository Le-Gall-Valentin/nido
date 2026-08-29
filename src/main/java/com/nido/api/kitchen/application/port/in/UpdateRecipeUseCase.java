package com.nido.api.kitchen.application.port.in;

import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.space.domain.model.SpaceMembership;

public interface UpdateRecipeUseCase {
    Recipe update(UpdateRecipeCommand command, SpaceMembership caller);
}
