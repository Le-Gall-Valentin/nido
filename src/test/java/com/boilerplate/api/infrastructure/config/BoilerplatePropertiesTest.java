package com.boilerplate.api.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoilerplatePropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void jwtProperties_rejectsZeroExpiryMinutes() {
        var props = new BoilerplateProperties.JwtProperties("valid-secret", 0, "boilerplate", "boilerplate");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void jwtProperties_rejectsNegativeExpiryMinutes() {
        var props = new BoilerplateProperties.JwtProperties("valid-secret", -5, "boilerplate", "boilerplate");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void jwtProperties_acceptsPositiveExpiryMinutes() {
        var props = new BoilerplateProperties.JwtProperties("valid-secret", 15, "boilerplate", "boilerplate");
        var violations = validator.validate(props);
        assertThat(violations).isEmpty();
    }

    @Test
    void seedProperties_rejectsPasswordShorterThan8Chars() {
        var props = new BoilerplateProperties.SeedProperties("admin", "admin@test.com", "short");
        var violations = validator.validate(props);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password"))).isTrue();
    }

    @Test
    void seedProperties_acceptsPasswordOf8CharsOrMore() {
        var props = new BoilerplateProperties.SeedProperties("admin", "admin@test.com", "strongpw");
        var violations = validator.validate(props);
        assertThat(violations).isEmpty();
    }
}