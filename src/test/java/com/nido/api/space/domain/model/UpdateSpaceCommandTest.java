package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateSpaceCommandTest {

    private static final UUID SPACE_ID = UUID.randomUUID();

    @Test
    void an_all_null_command_is_a_no_op() {
        UpdateSpaceCommand command = new UpdateSpaceCommand(SPACE_ID, null, null, null, null);

        assertThat(command.name()).isNull();
        assertThat(command.description()).isNull();
        assertThat(command.accent()).isNull();
        assertThat(command.glyph()).isNull();
    }

    @Test
    void a_name_only_command_leaves_the_other_fields_absent() {
        UpdateSpaceCommand command =
            new UpdateSpaceCommand(SPACE_ID, "  Nouveau nom  ", null, null, null);

        assertThat(command.name()).isEqualTo("Nouveau nom");
        assertThat(command.description()).isNull();
        assertThat(command.accent()).isNull();
        assertThat(command.glyph()).isNull();
    }

    @Test
    void a_blank_description_means_clear_it() {
        // La chaîne vide est le seul moyen de distinguer « effacer » de « laisser tel quel ».
        assertThat(new UpdateSpaceCommand(SPACE_ID, null, "   ", null, null).description()).isEmpty();
        assertThat(new UpdateSpaceCommand(SPACE_ID, null, "", null, null).description()).isEmpty();
    }

    @Test
    void a_provided_description_is_trimmed() {
        UpdateSpaceCommand command =
            new UpdateSpaceCommand(SPACE_ID, null, "  Notre appart  ", null, null);

        assertThat(command.description()).isEqualTo("Notre appart");
    }

    @Test
    void rejects_an_accent_outside_the_palette() {
        assertThatThrownBy(() -> new UpdateSpaceCommand(SPACE_ID, null, null, "#123456", null))
            .isInstanceOf(SpaceException.InvalidAppearance.class);
    }

    @Test
    void rejects_a_glyph_outside_the_allowed_list() {
        assertThatThrownBy(() -> new UpdateSpaceCommand(SPACE_ID, null, null, null, "💀"))
            .isInstanceOf(SpaceException.InvalidAppearance.class);
    }

    @Test
    void rejects_a_blank_name_because_the_name_cannot_be_cleared() {
        assertThatThrownBy(() -> new UpdateSpaceCommand(SPACE_ID, "   ", null, null, null))
            .isInstanceOf(SpaceException.InvalidSpaceName.class);
    }

    @Test
    void rejects_a_name_longer_than_80_characters() {
        String tooLong = "x".repeat(81);

        assertThatThrownBy(() -> new UpdateSpaceCommand(SPACE_ID, tooLong, null, null, null))
            .isInstanceOf(SpaceException.InvalidSpaceName.class);
    }

    @Test
    void rejects_a_description_longer_than_280_characters() {
        String tooLong = "x".repeat(281);

        assertThatThrownBy(() -> new UpdateSpaceCommand(SPACE_ID, null, tooLong, null, null))
            .isInstanceOf(SpaceException.InvalidSpaceDescription.class);
    }

    @Test
    void requires_a_space_id() {
        assertThatThrownBy(() -> new UpdateSpaceCommand(null, "Nouveau nom", null, null, null))
            .isInstanceOf(NullPointerException.class);
    }
}
