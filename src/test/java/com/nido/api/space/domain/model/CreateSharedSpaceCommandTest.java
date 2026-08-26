package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateSharedSpaceCommandTest {

    private static final UUID CREATOR = UUID.randomUUID();

    @Test
    void trims_name_and_description() {
        CreateSharedSpaceCommand command =
            new CreateSharedSpaceCommand("  Chez Valentin  ", "  Notre appart  ", "#c17a5c", "🏡", CREATOR);

        assertThat(command.name()).isEqualTo("Chez Valentin");
        assertThat(command.description()).isEqualTo("Notre appart");
    }

    @Test
    void turns_blank_description_into_null() {
        CreateSharedSpaceCommand command =
            new CreateSharedSpaceCommand("Chez Valentin", "   ", "#c17a5c", "🏡", CREATOR);

        assertThat(command.description()).isNull();
    }

    @Test
    void rejects_blank_name() {
        assertThatThrownBy(() -> new CreateSharedSpaceCommand("   ", null, "#c17a5c", "🏡", CREATOR))
            .isInstanceOf(SpaceException.InvalidSpaceName.class);
    }

    @Test
    void rejects_name_longer_than_80_characters() {
        String tooLong = "x".repeat(81);

        assertThatThrownBy(() -> new CreateSharedSpaceCommand(tooLong, null, "#c17a5c", "🏡", CREATOR))
            .isInstanceOf(SpaceException.InvalidSpaceName.class);
    }

    @Test
    void rejects_description_longer_than_280_characters() {
        String tooLong = "x".repeat(281);

        assertThatThrownBy(() -> new CreateSharedSpaceCommand("Chez Valentin", tooLong, "#c17a5c", "🏡", CREATOR))
            .isInstanceOf(SpaceException.InvalidSpaceDescription.class);
    }

    @Test
    void rejects_appearance_outside_the_palette() {
        assertThatThrownBy(() -> new CreateSharedSpaceCommand("Chez Valentin", null, "#123456", "🏡", CREATOR))
            .isInstanceOf(SpaceException.InvalidAppearance.class);
    }
}
