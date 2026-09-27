package com.nido.api.dashboard.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingAislesTest {

    private final ShoppingAisles.Aisle fruits = new ShoppingAisles.Aisle(UUID.randomUUID(), "Fruits et légumes", 0, false);
    private final ShoppingAisles.Aisle grocery = new ShoppingAisles.Aisle(UUID.randomUUID(), "Épicerie", 1, false);
    private final ShoppingAisles.Aisle other = new ShoppingAisles.Aisle(UUID.randomUUID(), "Autre", 9, true);

    @Test
    void groupsFollowTheAisleOrderWithTheFirstThreeNamesInListOrder() {
        ShoppingCard card = card(List.of(
            item(grocery, "Pâtes", 4, false),
            item(fruits, "Pommes", 3, false),
            item(fruits, "Tomates", 0, false),
            item(fruits, "Courgettes", 1, false),
            item(fruits, "Poires", 2, false)), List.of(other, grocery, fruits));

        assertThat(card.remaining()).isEqualTo(5);
        assertThat(card.categories()).containsExactly(
            new ShoppingGroup(fruits.id(), "Fruits et légumes", 4, List.of("Tomates", "Courgettes", "Poires")),
            new ShoppingGroup(grocery.id(), "Épicerie", 1, List.of("Pâtes")));
    }

    @Test
    void doneItemsAreNeitherCountedNorShown() {
        ShoppingCard card = card(List.of(item(fruits, "Tomates", 0, true), item(grocery, "Café", 1, false)),
            List.of(fruits, grocery, other));

        assertThat(card.remaining()).isEqualTo(1);
        assertThat(card.categories()).extracting(ShoppingGroup::name).containsExactly("Épicerie");
    }

    @Test
    void anItemWhoseAisleIsGoneFallsIntoTheFallbackAisle() {
        ShoppingAisles.Aisle deleted = new ShoppingAisles.Aisle(UUID.randomUUID(), "Supprimée", 5, false);

        ShoppingCard card = card(List.of(item(deleted, "Piles", 0, false), item(other, "Ampoules", 1, false)),
            List.of(fruits, other));

        assertThat(card.remaining()).isEqualTo(2);
        assertThat(card.categories()).containsExactly(new ShoppingGroup(other.id(), "Autre", 2, List.of("Piles", "Ampoules")));
    }

    @Test
    void nothingLeftToBuyMeansNoCardAndTheAislesAreNeverNeeded() {
        Supplier<List<ShoppingAisles.Aisle>> aisles = () -> {
            throw new AssertionError("the aisles were read for a list with nothing left to buy");
        };

        SourceResult result = ShoppingAisles.of(List.of(item(fruits, "Tomates", 0, true)), aisles);

        assertThat(result.card()).isNull();
        assertThat(result.attention()).isEmpty();
    }

    private static ShoppingCard card(List<ShoppingAisles.Item> items, List<ShoppingAisles.Aisle> aisles) {
        return (ShoppingCard) ShoppingAisles.of(items, () -> aisles).card();
    }

    private static ShoppingAisles.Item item(ShoppingAisles.Aisle aisle, String name, int position, boolean done) {
        return new ShoppingAisles.Item(name, aisle.id(), position, done);
    }
}
