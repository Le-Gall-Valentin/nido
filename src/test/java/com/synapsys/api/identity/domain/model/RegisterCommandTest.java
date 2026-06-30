package com.synapsys.api.identity.domain.model;

import com.synapsys.api.shared.model.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegisterCommandTest {

    @Test
    void constructor_normalizesEmailToLowercase() {
        var cmd = new RegisterCommand("alice", "User@TEST.com", "pass", Role.USER);
        assertThat(cmd.email()).isEqualTo("user@test.com");
    }

    @Test
    void constructor_nullEmailIsToleratedWithoutNPE() {
        var cmd = new RegisterCommand("alice", null, "pass", Role.USER);
        assertThat(cmd.email()).isNull();
    }

    @Test
    void constructor_nullUsername_throwsNullPointerException() {
        assertThatThrownBy(() -> new RegisterCommand(null, "e@test.com", "pass", Role.USER))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullRawPassword_throwsNullPointerException() {
        assertThatThrownBy(() -> new RegisterCommand("alice", "e@test.com", null, Role.USER))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_nullRole_throwsNullPointerException() {
        assertThatThrownBy(() -> new RegisterCommand("alice", "e@test.com", "pass", null))
            .isInstanceOf(NullPointerException.class);
    }
}