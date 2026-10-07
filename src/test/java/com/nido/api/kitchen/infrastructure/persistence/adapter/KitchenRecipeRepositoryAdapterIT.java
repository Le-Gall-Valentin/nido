package com.nido.api.kitchen.infrastructure.persistence.adapter;

import com.nido.api.IntegrationTestConfig;
import com.nido.api.TestSpaces;
import com.nido.api.infrastructure.sealing.SealedValueRejected;
import com.nido.api.infrastructure.sealing.SpaceSealer;
import com.nido.api.infrastructure.sealing.SpaceSealers;
import com.nido.api.kitchen.domain.model.CreateRecipeCommand;
import com.nido.api.kitchen.domain.model.KitchenException;
import com.nido.api.shared.model.MeasurementUnit;
import com.nido.api.kitchen.domain.model.Recipe;
import com.nido.api.kitchen.domain.model.RecipeCategory;
import com.nido.api.kitchen.domain.model.RecipeIngredient;
import com.nido.api.kitchen.domain.model.UpdateRecipeCommand;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeIngredientEntity;
import com.nido.api.kitchen.infrastructure.persistence.entity.RecipeStepEntity;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.infrastructure.persistence.entity.SpaceEntity;
import com.nido.api.space.infrastructure.persistence.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTestConfig
class KitchenRecipeRepositoryAdapterIT {

    @Autowired KitchenRecipeRepositoryAdapter adapter;
    @Autowired SpaceJpaRepository spaceJpaRepository;
    @Autowired JdbcTemplate jdbc;
    @Autowired SpaceSealers sealers;

    private UUID spaceId;

    @BeforeEach
    void setUp() {
        spaceJpaRepository.deleteAll();
        SpaceEntity space = new SpaceEntity();
        space.setType(SpaceType.SHARED);
        TestSpaces.name(space, "Chez Valentin");
        space.setAccent("#c17a5c");
        space.setGlyph("🏡");
        spaceId = spaceJpaRepository.saveAndFlush(space).getId();
    }

    private CreateRecipeCommand bolognaise() {
        return new CreateRecipeCommand(spaceId, "Pâtes bolognaise", "Un classique familial.", RecipeCategory.PLAT, 35, 4,
            List.of(
                new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM),
                new RecipeIngredient("Oignon", BigDecimal.ONE, MeasurementUnit.PIECE)),
            List.of("Faire revenir l'oignon.", "Ajouter la sauce."), "Encore meilleur réchauffé.");
    }

    @Test
    void create_persists_ingredients_and_steps_in_order() {
        Recipe created = adapter.create(bolognaise());

        assertThat(created.name()).isEqualTo("Pâtes bolognaise");
        assertThat(created.description()).isEqualTo("Un classique familial.");
        assertThat(created.favorite()).isFalse();
        assertThat(created.ingredients()).extracting(RecipeIngredient::name).containsExactly("Pâtes", "Oignon");
        assertThat(created.steps()).containsExactly("Faire revenir l'oignon.", "Ajouter la sauce.");
        assertThat(created.note()).isEqualTo("Encore meilleur réchauffé.");
    }

    @Test
    void update_replaces_all_ingredients_and_steps() {
        Recipe created = adapter.create(bolognaise());

        Recipe updated = adapter.update(new UpdateRecipeCommand(created.id(), spaceId, "Pâtes bolo maison", "Version maison.",
            RecipeCategory.PLAT, 40, 4,
            List.of(new RecipeIngredient("Pâtes", BigDecimal.valueOf(400), MeasurementUnit.GRAM)),
            List.of("Une seule étape."), "Se congèle bien."));

        assertThat(updated.name()).isEqualTo("Pâtes bolo maison");
        assertThat(updated.description()).isEqualTo("Version maison.");
        assertThat(updated.ingredients()).hasSize(1);
        assertThat(updated.ingredients().get(0).quantity()).isEqualByComparingTo("400");
        assertThat(updated.steps()).containsExactly("Une seule étape.");
        assertThat(updated.note()).isEqualTo("Se congèle bien.");
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
        TestSpaces.name(otherSpace, "Autre groupe");
        otherSpace.setAccent("#4a7fa0");
        otherSpace.setGlyph("🌿");
        UUID otherSpaceId = spaceJpaRepository.saveAndFlush(otherSpace).getId();
        adapter.create(new CreateRecipeCommand(otherSpaceId, "Curry", null, RecipeCategory.VEGETARIAN, 30, 4,
            List.of(new RecipeIngredient("Riz", BigDecimal.valueOf(300), MeasurementUnit.GRAM)), List.of(), null));

        List<Recipe> found = adapter.findBySpaceId(spaceId);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).name()).isEqualTo("Pâtes bolognaise");
    }

    private CreateRecipeCommand simple(UUID space, String name, String description, List<String> steps, String note) {
        return new CreateRecipeCommand(space, name, description, RecipeCategory.DESSERT, 10, 1,
            List.of(new RecipeIngredient("Sucre", BigDecimal.TEN, MeasurementUnit.GRAM)), steps, note);
    }

    @Test
    void every_text_of_a_recipe_is_stored_sealed_with_the_key_of_its_space() {
        Recipe created = adapter.create(bolognaise());
        SpaceSealer sealer = sealers.forSpace(spaceId);

        Map<String, Object> recipe = jdbc.queryForMap(
            "SELECT name_encrypted, description_encrypted, note_encrypted FROM kitchen_recipes WHERE id = ?", created.id());
        assertThat((String) recipe.get("name_encrypted")).startsWith("v2:");
        assertThat(sealer.open(RecipeEntity.NAME, created.id(), (String) recipe.get("name_encrypted"))).isEqualTo("Pâtes bolognaise");
        assertThat(sealer.open(RecipeEntity.DESCRIPTION, created.id(), (String) recipe.get("description_encrypted")))
            .isEqualTo("Un classique familial.");
        assertThat(sealer.open(RecipeEntity.NOTE, created.id(), (String) recipe.get("note_encrypted"))).isEqualTo("Encore meilleur réchauffé.");
        assertThat(jdbc.query("SELECT id, name_encrypted FROM kitchen_recipe_ingredients WHERE recipe_id = ? ORDER BY position",
                (rs, rowNum) -> sealer.open(RecipeIngredientEntity.NAME, rs.getObject("id", UUID.class), rs.getString("name_encrypted")),
                created.id()))
            .containsExactly("Pâtes", "Oignon");
        assertThat(jdbc.query("SELECT id, text_encrypted FROM kitchen_recipe_steps WHERE recipe_id = ? ORDER BY position",
                (rs, rowNum) -> sealer.open(RecipeStepEntity.TEXT, rs.getObject("id", UUID.class), rs.getString("text_encrypted")),
                created.id()))
            .containsExactly("Faire revenir l'oignon.", "Ajouter la sauce.");
    }

    @Test
    void a_name_or_an_ingredient_copied_from_another_row_is_refused() {
        Recipe bolognaise = adapter.create(bolognaise());
        Recipe rice = adapter.create(simple(spaceId, "Riz", null, List.of("Cuire."), null));
        jdbc.update("UPDATE kitchen_recipes SET name_encrypted = (SELECT name_encrypted FROM kitchen_recipes WHERE id = ?) WHERE id = ?",
            bolognaise.id(), rice.id());
        jdbc.update("""
            UPDATE kitchen_recipe_ingredients SET name_encrypted =
              (SELECT name_encrypted FROM kitchen_recipe_ingredients WHERE recipe_id = ? AND position = 0)
            WHERE recipe_id = ? AND position = 1""", bolognaise.id(), bolognaise.id());

        assertThatThrownBy(() -> adapter.findById(rice.id())).isInstanceOf(SealedValueRejected.class);
        assertThatThrownBy(() -> adapter.findById(bolognaise.id())).isInstanceOf(SealedValueRejected.class);
    }

    @Test
    void an_empty_description_and_a_missing_note_come_back_as_they_were_written() {
        Recipe created = adapter.create(simple(spaceId, "Riz", "", List.of("Cuire."), null));

        Recipe reread = adapter.findById(created.id()).orElseThrow();

        assertThat(reread.description()).isEmpty();
        assertThat(reread.note()).isNull();
    }

    @Test
    void a_step_at_its_longest_with_emoji_comes_back_intact() {
        String longest = "🥚".repeat(1000);

        Recipe created = adapter.create(simple(spaceId, "Omelette", null, List.of(longest), null));

        assertThat(adapter.findById(created.id()).orElseThrow().steps()).containsExactly(longest);
    }

    @Test
    void a_recipe_created_in_another_space_is_sealed_with_that_space_key() {
        // How a recipe is moved or copied: MoveRecipeHandler and CopyRecipeHandler create it in the destination.
        SpaceEntity otherSpace = new SpaceEntity();
        otherSpace.setType(SpaceType.SHARED);
        TestSpaces.name(otherSpace, "Autre groupe");
        otherSpace.setAccent("#c17a5c");
        otherSpace.setGlyph("🏡");
        UUID otherSpaceId = spaceJpaRepository.saveAndFlush(otherSpace).getId();

        Recipe moved = adapter.create(simple(otherSpaceId, "Tarte Tatin", null, List.of("Caraméliser."), null));

        String stored = jdbc.queryForObject("SELECT name_encrypted FROM kitchen_recipes WHERE id = ?", String.class, moved.id());
        assertThat(sealers.forSpace(otherSpaceId).open(RecipeEntity.NAME, moved.id(), stored)).isEqualTo("Tarte Tatin");
        assertThatThrownBy(() -> sealers.forSpace(spaceId).open(RecipeEntity.NAME, moved.id(), stored))
            .isInstanceOf(SealedValueRejected.class);
    }
}
