package com.nido.api.kitchen.infrastructure.persistence.repository;

import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeStepJpaRepository extends JpaRepository<RecipeStepEntity, UUID> {
    List<RecipeStepEntity> findByRecipeIdOrderByPositionAsc(UUID recipeId);
    List<RecipeStepEntity> findByRecipeIdInOrderByRecipeIdAscPositionAsc(Collection<UUID> recipeIds);
    void deleteByRecipeId(UUID recipeId);
}
