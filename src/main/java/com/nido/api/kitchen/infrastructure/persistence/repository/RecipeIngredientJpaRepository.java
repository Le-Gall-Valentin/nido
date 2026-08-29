package com.nido.api.kitchen.infrastructure.persistence.repository;

import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeIngredientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeIngredientJpaRepository extends JpaRepository<RecipeIngredientEntity, UUID> {
    List<RecipeIngredientEntity> findByRecipeIdOrderByPositionAsc(UUID recipeId);
    List<RecipeIngredientEntity> findByRecipeIdInOrderByRecipeIdAscPositionAsc(Collection<UUID> recipeIds);
    void deleteByRecipeId(UUID recipeId);
}
