package com.nido.api.identity.infrastructure.web.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The request side of the rule: it answers exactly what the domain's Username accepts. */
class ValidUsernameTest {

    record Form(@ValidUsername String username) {}

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean accepts(String username) {
        return validator.validate(new Form(username)).isEmpty();
    }

    @Test
    void accepts_what_the_domain_accepts_and_refuses_the_rest() {
        assertThat(accepts("jane.doe")).isTrue();
        assertThat(accepts("a".repeat(50))).isTrue();
        assertThat(accepts("jane@home")).isFalse();
        assertThat(accepts("ab")).isFalse();
        assertThat(accepts("a".repeat(51))).isFalse();
        assertThat(accepts("   ")).isFalse();
        assertThat(accepts(null)).isFalse();
    }
}
