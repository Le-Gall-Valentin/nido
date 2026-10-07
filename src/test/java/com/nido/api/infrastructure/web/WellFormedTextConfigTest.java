package com.nido.api.infrastructure.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WellFormedTextConfigTest {

    @Test
    void whole_characters_are_well_formed_text() {
        for (String text : new String[] {"", "Lait", "Crème fraîche", "Œufs 🥚", "🥚🥛", "x".repeat(200)}) {
            assertThat(WellFormedTextConfig.holdsHalfACharacter(text)).as(text).isFalse();
        }
    }

    @Test
    void a_surrogate_without_its_pair_is_half_a_character() {
        for (String text : new String[] {"\ud83e", "Œufs \ud83e", "\udd5a", "\udd5a\ud83e", "\ud83e🥚", "🥚\udd5a"}) {
            assertThat(WellFormedTextConfig.holdsHalfACharacter(text)).as(text).isTrue();
        }
    }
}
