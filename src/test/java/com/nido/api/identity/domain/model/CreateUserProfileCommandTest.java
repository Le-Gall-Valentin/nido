package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The command that reaches the database: it cannot carry an account the database would refuse. */
class CreateUserProfileCommandTest {

    @Test
    void a_username_nido_refuses_never_reaches_the_database() {
        assertThatThrownBy(() -> new CreateUserProfileCommand("jane@home", "jane@test.com", Role.USER))
            .isInstanceOf(IdentityException.InvalidUsername.class);
    }

    @Test
    void the_address_is_kept_lower_case() {
        assertThat(new CreateUserProfileCommand("jane", "Jane@Test.com", Role.USER).email()).isEqualTo("jane@test.com");
    }
}
