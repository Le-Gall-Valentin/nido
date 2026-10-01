package com.nido.api.shared.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameTest {

    record Form(@Username String username) {}

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> problems(String username) {
        return validator.validate(new Form(username)).stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toSet());
    }

    @Test
    void an_ordinary_username_passes() {
        assertThat(problems("jane.doe")).isEmpty();
        assertThat(problems("a".repeat(50))).isEmpty();
    }

    @Test
    void an_at_sign_is_refused_because_sign_in_would_read_it_as_an_address() {
        assertThat(problems("jane@home")).contains("must not contain @");
    }

    @Test
    void the_length_and_blank_rules_still_hold() {
        assertThat(problems("ab")).isNotEmpty();
        assertThat(problems("a".repeat(51))).isNotEmpty();
        assertThat(problems("   ")).isNotEmpty();
        assertThat(problems(null)).isNotEmpty();
    }
}
