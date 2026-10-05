package com.nido.api.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NidoPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void seedProperties_rejectsPasswordShorterThan8Chars() {
        var props = new NidoProperties.SeedProperties("admin", "admin@test.com", "short");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"))).isTrue();
    }

    @Test
    void seedProperties_acceptsPasswordOf8CharsOrMore() {
        var props = new NidoProperties.SeedProperties("admin", "admin@test.com", "strongpw");
        var violations = validator.validate(props);
        assertThat(violations).isEmpty();
    }
}