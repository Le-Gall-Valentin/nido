package com.nido.api.kitchen.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingListAggregatorTest {

    private final ShoppingListAggregator aggregator = new ShoppingListAggregator();

    private static RecipeIngredient ing(String name, String qty, MeasurementUnit unit) {
        return new RecipeIngredient(name, new BigDecimal(qty), unit);
    }

    @Test
    void sums_the_same_ingredient_in_the_same_unit() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Riz", "150", MeasurementUnit.GRAM),
            ing("Riz", "300", MeasurementUnit.GRAM)));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).name()).isEqualTo("Riz");
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("450");
        assertThat(lines.get(0).unit()).isEqualTo(MeasurementUnit.GRAM);
    }

    @Test
    void sums_mass_across_compatible_units_and_upgrades_to_kilogram_past_1000_grams() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Pommes de terre", "800", MeasurementUnit.GRAM),
            ing("Pommes de terre", "0.4", MeasurementUnit.KILOGRAM)));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("1.2");
        assertThat(lines.get(0).unit()).isEqualTo(MeasurementUnit.KILOGRAM);
    }

    @Test
    void sums_volume_across_compatible_units_and_stays_in_milliliters_under_the_threshold() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Lait de coco", "400", MeasurementUnit.MILLILITER),
            ing("Lait de coco", "20", MeasurementUnit.CENTILITER)));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("600");
        assertThat(lines.get(0).unit()).isEqualTo(MeasurementUnit.MILLILITER);
    }

    @Test
    void sums_the_same_count_unit_but_never_merges_different_count_units() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Œufs", "3", MeasurementUnit.PIECE),
            ing("Œufs", "2", MeasurementUnit.PIECE),
            ing("Pain de campagne", "4", MeasurementUnit.SLICE)));

        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("5");
        assertThat(lines.get(0).unit()).isEqualTo(MeasurementUnit.PIECE);
        assertThat(lines.get(1).unit()).isEqualTo(MeasurementUnit.SLICE);
    }

    @Test
    void merges_a_plural_spelling_with_its_singular() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Oignon", "1", MeasurementUnit.PIECE),
            ing("Oignons", "2", MeasurementUnit.PIECE)));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("3");
    }

    @Test
    void merge_key_is_accent_and_case_insensitive() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("crème fraîche", "100", MeasurementUnit.MILLILITER),
            ing("Crème Fraîche", "100", MeasurementUnit.MILLILITER)));

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).quantity()).isEqualByComparingTo("200");
    }

    @Test
    void displayed_name_is_the_first_chronological_occurrence() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("oignons", "1", MeasurementUnit.PIECE),
            ing("Oignon", "1", MeasurementUnit.PIECE)));

        assertThat(lines.get(0).name()).isEqualTo("oignons");
    }

    @Test
    void does_not_merge_two_different_ingredients() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Riz", "150", MeasurementUnit.GRAM),
            ing("Farine", "150", MeasurementUnit.GRAM)));

        assertThat(lines).hasSize(2);
    }

    @Test
    void preserves_first_occurrence_order_across_output_lines() {
        List<ShoppingListLine> lines = aggregator.aggregate(List.of(
            ing("Farine", "100", MeasurementUnit.GRAM),
            ing("Riz", "100", MeasurementUnit.GRAM),
            ing("Farine", "50", MeasurementUnit.GRAM)));

        assertThat(lines).extracting(ShoppingListLine::name).containsExactly("Farine", "Riz");
    }
}
