package com.nido.api.kitchen.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecipeTest {

    private Recipe recipeFor4() {
        return new Recipe(UUID.randomUUID(), UUID.randomUUID(), "Pâtes bolognaise", RecipeCategory.PLAT,
            35, 4, false,
            List.of(
                new RecipeIngredient("Pâtes", BigDecimal.valueOf(500), MeasurementUnit.GRAM),
                new RecipeIngredient("Oignon", BigDecimal.ONE, MeasurementUnit.PIECE)),
            List.of("Faire revenir l'oignon.", "Ajouter la sauce."),
            Instant.now(), Instant.now());
    }

    @Test
    void scaledIngredients_multiplies_quantities_by_the_portion_ratio() {
        List<RecipeIngredient> scaled = recipeFor4().scaledIngredients(2);

        assertThat(scaled.get(0).name()).isEqualTo("Pâtes");
        assertThat(scaled.get(0).quantity()).isEqualByComparingTo("250");
        assertThat(scaled.get(1).quantity()).isEqualByComparingTo("0.5");
    }

    @Test
    void scaledIngredients_at_the_reference_portions_is_unchanged() {
        List<RecipeIngredient> scaled = recipeFor4().scaledIngredients(4);

        assertThat(scaled.get(0).quantity()).isEqualByComparingTo("500");
    }

    @Test
    void scaledIngredients_preserves_ingredient_order() {
        List<RecipeIngredient> scaled = recipeFor4().scaledIngredients(8);

        assertThat(scaled).extracting(RecipeIngredient::name).containsExactly("Pâtes", "Oignon");
    }
}
