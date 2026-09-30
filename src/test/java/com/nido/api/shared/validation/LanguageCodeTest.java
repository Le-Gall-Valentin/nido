package com.nido.api.shared.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageCodeTest {

    record Form(@LanguageCode String language) {}

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean accepts(String language) {
        return validator.validate(new Form(language)).isEmpty();
    }

    @Test
    void accepts_the_code_of_a_language_the_app_speaks() {
        assertThat(accepts("fr")).isTrue();
        assertThat(accepts("en")).isTrue();
    }

    @Test
    void refuses_anything_else() {
        assertThat(accepts("de")).isFalse();
        assertThat(accepts("FR")).isFalse();
        assertThat(accepts("fr-FR")).isFalse();
        assertThat(accepts("")).isFalse();
    }

    @Test
    void leaves_a_missing_value_to_NotNull() {
        assertThat(accepts(null)).isTrue();
    }
}
