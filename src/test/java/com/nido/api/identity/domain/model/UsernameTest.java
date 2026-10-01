package com.nido.api.identity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void a_name_keeps_the_case_it_was_given_without_its_surrounding_spaces() {
        assertThat(new Username("  Jane.Doe ").value()).isEqualTo("Jane.Doe");
    }

    @Test
    void three_to_fifty_characters() {
        assertThat(Username.isValid("abc")).isTrue();
        assertThat(Username.isValid("a".repeat(50))).isTrue();
        assertThat(Username.isValid("ab")).isFalse();
        assertThat(Username.isValid("a".repeat(51))).isFalse();
    }

    @Test
    void an_at_sign_is_refused_because_sign_in_would_read_it_as_an_address() {
        assertThat(Username.isValid("jane@home")).isFalse();
        assertThatThrownBy(() -> new Username("jane@home")).isInstanceOf(IdentityException.InvalidUsername.class);
    }

    @Test
    void blank_or_missing_is_no_username() {
        assertThat(Username.isValid("   ")).isFalse();
        assertThat(Username.isValid(null)).isFalse();
        assertThatThrownBy(() -> new Username(null)).isInstanceOf(NullPointerException.class);
    }
}
