package com.nido.api.kitchen.infrastructure.persistence.adapter;

import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.kitchen.domain.port.out.RecipeRepository;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeIngredientEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeStepEntity;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeIngredientJpaRepository;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeJpaRepository;
import com.nido.api.kitchen.infrastructure.persistence.repository.RecipeStepJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class KitchenRecipeRepositoryAdapter implements RecipeRepository {

    private final RecipeJpaRepository recipes;
    private final RecipeIngredientJpaRepository ingredients;
    private final RecipeStepJpaRepository steps;

    public KitchenRecipeRepositoryAdapter(RecipeJpaRepository recipes,
                                          RecipeIngredientJpaRepository ingredients,
                                          RecipeStepJpaRepository steps) {
        this.recipes = recipes;
        this.ingredients = ingredients;
        this.steps = steps;
    }

    @Override
    public Optional<Recipe> findById(UUID recipeId) {
        return recipes.findById(recipeId).map(this::toDomain);
    }

    @Override
    public List<Recipe> findBySpaceId(UUID spaceId) {
        return toDomainList(recipes.findBySpaceIdOrderByNameAsc(spaceId));
    }

    @Override
    public List<Recipe> findByIds(Collection<UUID> recipeIds) {
        return recipeIds.isEmpty() ? List.of() : toDomainList(recipes.findAllById(recipeIds));
    }

    @Override
    @Transactional
    public Recipe create(CreateRecipeCommand command) {
        RecipeEntity e = new RecipeEntity();
        e.setSpaceId(command.spaceId());
        e.setName(command.name());
        e.setCategory(command.category());
        e.setMinutes(command.minutes());
        e.setReferencePortions(command.referencePortions());
        e.setFavorite(false);
        RecipeEntity saved = recipes.saveAndFlush(e);
        saveIngredientsAndSteps(saved.getId(), command.ingredients(), command.steps());
        return findById(saved.getId()).orElseThrow(KitchenException.RecipeNotFound::new);
    }

    @Override
    @Transactional
    public Recipe update(UpdateRecipeCommand command) {
        RecipeEntity e = recipes.findById(command.recipeId()).orElseThrow(KitchenException.RecipeNotFound::new);
        e.setName(command.name());
        e.setCategory(command.category());
        e.setMinutes(command.minutes());
        e.setReferencePortions(command.referencePortions());
        recipes.saveAndFlush(e);
        ingredients.deleteByRecipeId(e.getId());
        steps.deleteByRecipeId(e.getId());
        saveIngredientsAndSteps(e.getId(), command.ingredients(), command.steps());
        return findById(e.getId()).orElseThrow(KitchenException.RecipeNotFound::new);
    }

    @Override
    public void delete(UUID recipeId) {
        recipes.deleteById(recipeId);
        recipes.flush();
    }

    @Override
    public void setFavorite(UUID recipeId, boolean favorite) {
        RecipeEntity e = recipes.findById(recipeId).orElseThrow(KitchenException.RecipeNotFound::new);
        e.setFavorite(favorite);
        recipes.saveAndFlush(e);
    }

    private void saveIngredientsAndSteps(UUID recipeId, List<RecipeIngredient> ingredientList, List<String> stepList) {
        for (int i = 0; i < ingredientList.size(); i++) {
            RecipeIngredient ri = ingredientList.get(i);
            RecipeIngredientEntity ie = new RecipeIngredientEntity();
            ie.setRecipeId(recipeId);
            ie.setPosition(i);
            ie.setName(ri.name());
            ie.setQuantity(ri.quantity());
            ie.setUnit(ri.unit());
            ingredients.save(ie);
        }
        for (int i = 0; i < stepList.size(); i++) {
            RecipeStepEntity se = new RecipeStepEntity();
            se.setRecipeId(recipeId);
            se.setPosition(i);
            se.setText(stepList.get(i));
            steps.save(se);
        }
        ingredients.flush();
        steps.flush();
    }

    private List<Recipe> toDomainList(List<RecipeEntity> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = found.stream().map(RecipeEntity::getId).toList();
        Map<UUID, List<RecipeIngredientEntity>> ingredientsByRecipe = ingredients
            .findByRecipeIdInOrderByRecipeIdAscPositionAsc(ids).stream()
            .collect(Collectors.groupingBy(RecipeIngredientEntity::getRecipeId));
        Map<UUID, List<RecipeStepEntity>> stepsByRecipe = steps
            .findByRecipeIdInOrderByRecipeIdAscPositionAsc(ids).stream()
            .collect(Collectors.groupingBy(RecipeStepEntity::getRecipeId));
        return found.stream()
            .map(e -> toDomain(e,
                ingredientsByRecipe.getOrDefault(e.getId(), List.of()),
                stepsByRecipe.getOrDefault(e.getId(), List.of())))
            .toList();
    }

    private Recipe toDomain(RecipeEntity e) {
        return toDomain(e,
            ingredients.findByRecipeIdOrderByPositionAsc(e.getId()),
            steps.findByRecipeIdOrderByPositionAsc(e.getId()));
    }

    private Recipe toDomain(RecipeEntity e, List<RecipeIngredientEntity> ingredientEntities, List<RecipeStepEntity> stepEntities) {
        return new Recipe(e.getId(), e.getSpaceId(), e.getName(), e.getCategory(), e.getMinutes(), e.getReferencePortions(),
            e.isFavorite(),
            ingredientEntities.stream().map(i -> new RecipeIngredient(i.getName(), i.getQuantity(), i.getUnit())).toList(),
            stepEntities.stream().map(RecipeStepEntity::getText).toList(),
            e.getCreatedAt(), e.getUpdatedAt());
    }
}
