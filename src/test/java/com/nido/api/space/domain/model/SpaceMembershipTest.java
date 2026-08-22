package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceMembershipTest {

    @Test
    void viewer_cannot_write_nor_manage() {
        SpaceMembership viewer = membership(SpaceRole.VIEWER);

        assertThatThrownBy(viewer::ensureCanWrite)
            .isInstanceOf(SpaceException.InsufficientRole.class);
        assertThatThrownBy(viewer::ensureCanManageSpace)
            .isInstanceOf(SpaceException.InsufficientRole.class);
        assertThatThrownBy(viewer::ensureOwner)
            .isInstanceOf(SpaceException.OwnerRequired.class);
    }

    @Test
    void member_can_write_but_cannot_manage() {
        SpaceMembership member = membership(SpaceRole.MEMBER);

        assertThatCode(member::ensureCanWrite).doesNotThrowAnyException();
        assertThatThrownBy(member::ensureCanManageSpace)
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void admin_can_manage_but_is_not_owner() {
        SpaceMembership admin = membership(SpaceRole.ADMIN);

        assertThatCode(admin::ensureCanWrite).doesNotThrowAnyException();
        assertThatCode(admin::ensureCanManageSpace).doesNotThrowAnyException();
        assertThat(admin.isOwner()).isFalse();
        assertThatThrownBy(admin::ensureOwner)
            .isInstanceOf(SpaceException.OwnerRequired.class);
    }

    @Test
    void owner_can_do_everything() {
        SpaceMembership owner = membership(SpaceRole.OWNER);

        assertThatCode(owner::ensureCanWrite).doesNotThrowAnyException();
        assertThatCode(owner::ensureCanManageSpace).doesNotThrowAnyException();
        assertThatCode(owner::ensureOwner).doesNotThrowAnyException();
        assertThat(owner.isOwner()).isTrue();
    }

    @Test
    void ensureSameSpace_accepts_its_own_space_and_refuses_any_other() {
        SpaceMembership membership = membership(SpaceRole.OWNER);

        assertThatCode(() -> membership.ensureSameSpace(membership.spaceId()))
            .doesNotThrowAnyException();
        // Un autre contexte est traité comme une absence d'adhésion : 404, pas 403.
        assertThatThrownBy(() -> membership.ensureSameSpace(UUID.randomUUID()))
            .isInstanceOf(SpaceException.NotAMember.class);
    }

    private static SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), role, Instant.now());
    }
}
