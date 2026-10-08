package com.nido.api.authentication.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaskedEmailTest {

    @Test
    void the_first_and_last_letters_stay_around_six_dots_and_the_domain_is_whole() {
        assertThat(MaskedEmail.of("camille.martin@exemple.fr")).isEqualTo("c••••••n@exemple.fr");
    }

    @Test
    void the_dots_never_tell_the_length() {
        assertThat(MaskedEmail.of("jo@exemple.fr")).isEqualTo("j••••••@exemple.fr");
        assertThat(MaskedEmail.of("abc@exemple.fr")).isEqualTo("a••••••c@exemple.fr");
        assertThat(MaskedEmail.of("a-very-long-local-part@exemple.fr")).isEqualTo("a••••••t@exemple.fr");
    }

    @Test
    void a_letter_outside_the_basic_plane_is_kept_whole() {
        assertThat(MaskedEmail.of("😀bob😀@exemple.fr")).isEqualTo("😀••••••😀@exemple.fr");
    }
}
