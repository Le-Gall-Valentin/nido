package com.nido.api.shared.validation;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordTest {

    record Form(@StrongPassword String password) {}

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private boolean accepts(String password) {
        return validator.validate(new Form(password)).isEmpty();
    }

    @Test
    void accepts_eight_to_seventy_two_characters_with_an_uppercase_a_digit_and_a_symbol() {
        assertThat(accepts("LongEnough1!")).isTrue();
        assertThat(accepts("Aa1!" + "x".repeat(68))).isTrue();
    }

    @Test
    void refuses_what_the_account_page_refuses() {
        assertThat(accepts(null)).isFalse();
        assertThat(accepts("   ")).isFalse();
        assertThat(accepts("Short1!")).isFalse();
        assertThat(accepts("Aa1!" + "x".repeat(69))).isFalse();
        assertThat(accepts("longenough1!")).isFalse();
        assertThat(accepts("LongEnough!!")).isFalse();
        assertThat(accepts("LongEnough11")).isFalse();
    }

    @Test
    void refuses_what_bcrypt_cannot_read_whole_even_under_seventy_two_characters() {
        // 72 characters but 141 bytes: bcrypt reads 72 bytes, and hashing more throws — which was a 500.
        assertThat(accepts("Aé1!" + "é".repeat(68))).isFalse();
    }

    @Test
    void seventy_two_bytes_is_the_limit_whatever_the_characters_count() {
        assertThat(accepts("Aé1!" + "x".repeat(67))).isTrue();
        assertThat(accepts("Aé1!" + "x".repeat(68))).isFalse();
    }
}
