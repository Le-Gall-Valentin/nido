package com.nido.api.kitchen.domain.port.out;

import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeRepository {
    Optional<Recipe> findById(UUID recipeId);
    List<Recipe> findBySpaceId(UUID spaceId);
    List<Recipe> findByIds(Collection<UUID> recipeIds);
    Recipe create(CreateRecipeCommand command);
    Recipe update(UpdateRecipeCommand command);
    void delete(UUID recipeId);
    void setFavorite(UUID recipeId, boolean favorite);
}
