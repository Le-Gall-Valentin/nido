package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceTest {

    @Test
    void personal_space_is_immutable() {
        Space space = personal();

        assertThat(space.isPersonal()).isTrue();
        assertThatThrownBy(space::ensureShared)
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
    }

    @Test
    void shared_space_can_be_mutated() {
        Space space = shared();

        assertThat(space.isPersonal()).isFalse();
        assertThatCode(space::ensureShared).doesNotThrowAnyException();
    }

    @Test
    void appearance_must_come_from_the_allowed_palette() {
        assertThatThrownBy(() -> SpaceAppearance.ensureValid("#000000", "🏡"))
            .isInstanceOf(SpaceException.InvalidAppearance.class);
        assertThatThrownBy(() -> SpaceAppearance.ensureValid("#5c7a58", "💀"))
            .isInstanceOf(SpaceException.InvalidAppearance.class);
        assertThatCode(() -> SpaceAppearance.ensureValid("#5c7a58", "🏡"))
            .doesNotThrowAnyException();
    }

    @Test
    void personal_appearance_constants_match_the_design() {
        assertThat(SpaceAppearance.PERSONAL_ACCENT).isEqualTo("#8a7d6b");
        assertThat(SpaceAppearance.PERSONAL_GLYPH).isEqualTo("👤");
        assertThat(SpaceAppearance.ACCENTS).hasSize(6).doesNotContain(SpaceAppearance.PERSONAL_ACCENT);
    }

    @Test
    void role_ranks_are_strictly_increasing_in_power() {
        assertThat(SpaceRole.VIEWER.rank()).isLessThan(SpaceRole.MEMBER.rank());
        assertThat(SpaceRole.MEMBER.rank()).isLessThan(SpaceRole.ADMIN.rank());
        assertThat(SpaceRole.ADMIN.rank()).isLessThan(SpaceRole.OWNER.rank());
    }

    @Test
    void atLeast_compares_a_role_against_a_required_floor() {
        assertThat(SpaceRole.VIEWER.atLeast(SpaceRole.VIEWER)).isTrue();
        assertThat(SpaceRole.VIEWER.atLeast(SpaceRole.MEMBER)).isFalse();
        assertThat(SpaceRole.MEMBER.atLeast(SpaceRole.MEMBER)).isTrue();
        assertThat(SpaceRole.MEMBER.atLeast(SpaceRole.ADMIN)).isFalse();
        assertThat(SpaceRole.ADMIN.atLeast(SpaceRole.MEMBER)).isTrue();
        assertThat(SpaceRole.OWNER.atLeast(SpaceRole.OWNER)).isTrue();
    }

    private static Space personal() {
        return new Space(UUID.randomUUID(), SpaceType.PERSONAL, "Perso", null,
            SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, UUID.randomUUID(), Instant.now());
    }

    private static Space shared() {
        return new Space(UUID.randomUUID(), SpaceType.SHARED, "Chez Valentin", "Notre appartement à trois",
            "#c17a5c", "🏡", null, Instant.now());
    }
}
