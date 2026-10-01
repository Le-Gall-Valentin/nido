package com.nido.api.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateProfileCommandTest {

    @Test
    void constructor_normalizesEmailToLowercase() {
        var cmd = new UpdateProfileCommand(UUID.randomUUID(), "alice", "Carol@TEST.com", null);
        assertThat(cmd.email()).isEqualTo("carol@test.com");
    }

    @Test
    void a_username_nido_refuses_never_reaches_the_database() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new UpdateProfileCommand(UUID.randomUUID(), "jane@home", "jane@test.com", null))
            .isInstanceOf(IdentityException.InvalidUsername.class);
    }

    @Test
    void constructor_nullEmailIsToleratedWithoutNPE() {
        var cmd = new UpdateProfileCommand(UUID.randomUUID(), "alice", null, null);
        assertThat(cmd.email()).isNull();
    }
}
