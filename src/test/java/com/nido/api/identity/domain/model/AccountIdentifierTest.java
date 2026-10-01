package com.nido.api.identity.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountIdentifierTest {

    @Test
    void an_at_sign_makes_it_an_address() {
        assertThat(AccountIdentifier.parse(" Jane@X.FR "))
            .contains(new AccountIdentifier.ByEmail(new EmailAddress("jane@x.fr")));
    }

    @Test
    void without_one_it_is_a_username_as_typed() {
        assertThat(AccountIdentifier.parse(" Jane ")).contains(new AccountIdentifier.ByUsername("Jane"));
    }

    @Test
    void blank_names_nobody() {
        assertThat(AccountIdentifier.parse("   ")).isEmpty();
        assertThat(AccountIdentifier.parse(null)).isEmpty();
    }
}
