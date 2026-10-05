package com.nido.api.identity.domain.model;

import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterCommandTest {

    @Test
    void constructor_normalizesEmailToLowercase() {
        var cmd = new RegisterCommand("alice", "User@TEST.com", Role.USER);
        assertThat(cmd.email()).isEqualTo("user@test.com");
    }

    @Test
    void a_username_nido_refuses_is_stopped_at_the_command() {
        assertThatThrownBy(() -> new RegisterCommand("jane@home", "jane@test.com", Role.USER))
            .isInstanceOf(IdentityException.InvalidUsername.class);
    }

    @Test
    void its_string_form_never_shows_the_address() {
        assertThat(new RegisterCommand("alice", "alice@test.com", Role.USER).toString()).doesNotContain("alice@test.com");
    }

    @Test
    void constructor_nullEmailIsToleratedWithoutNPE() {
        var cmd = new RegisterCommand("alice", null, Role.USER);
        assertThat(cmd.email()).isNull();
    }

    @Test
    void constructor_nullUsername_throwsNullPointerException() {
        assertThatThrownBy(() -> new RegisterCommand(null, "e@test.com", Role.USER))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullRole_throwsNullPointerException() {
        assertThatThrownBy(() -> new RegisterCommand("alice", "e@test.com", null))
            .isInstanceOf(NullPointerException.class);
    }
}
