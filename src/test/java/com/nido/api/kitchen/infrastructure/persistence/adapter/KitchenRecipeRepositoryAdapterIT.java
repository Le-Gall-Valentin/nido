package com.nido.api.kitchen.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.kitchen.domain.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class KitchenRecipeRepositoryAdapterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired KitchenRecipeRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        space.setName("Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
    }

    private CreateRecipeCommand bolognaise() {
        return new CreateRecipeCommand(spaceId, "Pâtes bolognaise", RecipeCategory.PLAT, 35, 4,
            List.of(
                new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM),
                new RecipeIngredient("Oignon", BigDecimal.ONE, MeasurementUnit.PIECE)),
            List.of("Faire revenir l'oignon.", "Ajouter la sauce."));
    }

    @Test
    void create_persists_ingredients_and_steps_in_order() {
        Recipe created = adapter.create(bolognaise());

        assertThat(created.name()).isEqualTo("Pâtes bolognaise");
        assertThat(created.favorite()).isFalse();
        assertThat(created.ingredients()).extracting(RecipeIngredient::name).containsExactly("Pâtes", "Oignon");
        assertThat(created.steps()).containsExactly("Faire revenir l'oignon.", "Ajouter la sauce.");
    }

    @Test
    void update_replaces_all_ingredients_and_steps() {
        Recipe created = adapter.create(bolognaise());

        Recipe updated = adapter.update(new UpdateRecipeCommand(created.id(), spaceId, "Pâtes bolo maison",
            RecipeCategory.PLAT, 40, 4,
            List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(400), MeasurementUnit.GRAM)),
            List.of("Une seule étape.")));

        assertThat(updated.name()).isEqualTo("Pâtes bolo maison");
        assertThat(updated.ingredients()).hasSize(1);
        assertThat(updated.ingredients().get(0).quantity()).isEqualByComparingTo("400");
        assertThat(updated.steps()).containsExactly("Une seule étape.");
    }

    @Test
    void delete_removes_the_recipe() {
        Recipe created = adapter.create(bolognaise());

        adapter.delete(created.id());

        assertThat(adapter.findById(created.id())).isEmpty();
    }

    @Test
    void setFavorite_toggles_the_flag() {
        Recipe created = adapter.create(bolognaise());

        adapter.setFavorite(created.id(), true);

        assertThat(adapter.findById(created.id())).get().extracting(Recipe::favorite).isEqualTo(true);
    }

    @Test
    void setFavorite_on_a_missing_recipe_throws_not_found() {
        assertThatThrownBy(() -> adapter.setFavorite(UUID.randomUUID(), true))
            .isInstanceOf(KitchenException.RecipeNotFound.class);
    }

    @Test
    void findBySpaceId_returns_only_that_spaces_recipes() {
        adapter.create(bolognaise());
        SpaceEntity otherSpace = new SpaceEntity();
        otherSpace.setType(SpaceType.SHARED);
        otherSpace.setName("Autre groupe");
        otherSpace.setAccent("#4a7fa0");
        otherSpace.setGlyph("🌿");
        UUID otherSpaceId = spaceJpaRepository.saveAndFlush(otherSpace).getId();
        adapter.create(new CreateRecipeCommand(otherSpaceId, "Curry", RecipeCategory.VEGETARIAN, 30, 4,
            List.of(new RecipeIngredient("Riz", BigDecimal.valueOf(300), MeasurementUnit.GRAM)), List.of()));

        List<Recipe> found = adapter.findBySpaceId(spaceId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).name()).isEqualTo("Pâtes bolognaise");
    }
}
