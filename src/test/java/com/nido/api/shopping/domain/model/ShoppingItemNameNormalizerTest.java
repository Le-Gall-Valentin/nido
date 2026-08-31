package com.nido.api.shopping.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShoppingItemNameNormalizerTest {

    @Test
    void trims_lowercases_and_collapses_whitespace() {
        assertThat(ShoppingItemNameNormalizer.normalize("  Pommes   de   Terre  ")).isEqualTo("pommes de terre");
    }

    @Test
    void strips_diacritics() {
        assertThat(ShoppingItemNameNormalizer.normalize("Crème fraîche")).isEqualTo("creme fraiche");
    }

    @Test
    void folds_a_trailing_plural_s_when_the_word_is_long_enough() {
        assertThat(ShoppingItemNameNormalizer.normalize("Oignons")).isEqualTo("oignon");
    }

    @Test
    void does_not_fold_a_trailing_s_on_a_short_word() {
        assertThat(ShoppingItemNameNormalizer.normalize("Riz")).isEqualTo("riz");
    }

    @Test
    void two_differently_cased_and_accented_spellings_normalize_to_the_same_key() {
        assertThat(ShoppingItemNameNormalizer.normalize("Poulet"))
            .isEqualTo(ShoppingItemNameNormalizer.normalize("  POULET  "));
    }
}
