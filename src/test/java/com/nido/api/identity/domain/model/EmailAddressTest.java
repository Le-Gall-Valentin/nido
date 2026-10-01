package com.nido.api.identity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailAddressTest {

    @Test
    void an_address_is_kept_lower_case_without_its_surrounding_spaces() {
        assertThat(new EmailAddress(" Jane.Doe@Example.FR ").value()).isEqualTo("jane.doe@example.fr");
    }

    @Test
    void normalize_lets_a_missing_address_through() {
        assertThat(EmailAddress.normalize(null)).isNull();
        assertThat(EmailAddress.normalize("A@B.C")).isEqualTo("a@b.c");
    }
}
