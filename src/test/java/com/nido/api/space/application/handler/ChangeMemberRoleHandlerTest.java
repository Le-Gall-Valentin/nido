package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.ChangeMemberRoleCommand;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeMemberRoleHandlerTest {

    @Mock SpaceMembershipPort spaceMembershipPort;

    private ChangeMemberRoleHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ChangeMemberRoleHandler(spaceMembershipPort);
    }

    @Test
    void an_admin_can_promote_a_member_to_admin() {
        SpaceMembership target = membership(targetId, SpaceRole.MEMBER);
        when(spaceMembershipPort.find(spaceId, targetId)).thenReturn(Optional.of(target));

        handler.change(new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.ADMIN),
            membership(callerId, SpaceRole.ADMIN));

        verify(spaceMembershipPort).changeRole(target.id(), SpaceRole.ADMIN);
    }

    @Test
    void a_member_cannot_change_roles() {
        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.ADMIN),
                membership(callerId, SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void nobody_changes_their_own_role() {
        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, callerId, SpaceRole.MEMBER),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.SelfManagementForbidden.class);
    }

    @Test
    void the_owner_role_is_not_assignable_here() {
        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.OWNER),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.OwnerRoleNotAssignable.class);
    }

    @Test
    void the_owner_membership_is_protected() {
        when(spaceMembershipPort.find(spaceId, targetId))
            .thenReturn(Optional.of(membership(targetId, SpaceRole.OWNER)));

        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.MEMBER),
                membership(callerId, SpaceRole.ADMIN)))
            .isInstanceOf(SpaceException.OwnerMembershipProtected.class);
    }

    @Test
    void a_no_op_role_change_is_rejected() {
        when(spaceMembershipPort.find(spaceId, targetId))
            .thenReturn(Optional.of(membership(targetId, SpaceRole.ADMIN)));

        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.ADMIN),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.RoleAlreadyAssigned.class);
    }

    @Test
    void an_unknown_member_is_not_found() {
        when(spaceMembershipPort.find(spaceId, targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(spaceId, targetId, SpaceRole.ADMIN),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.MemberNotFound.class);
        verify(spaceMembershipPort, never()).changeRole(any(), any());
    }

    @Test
    void a_command_targeting_another_space_than_the_authorized_one_is_refused() {
        assertThatThrownBy(() -> handler.change(
                new ChangeMemberRoleCommand(UUID.randomUUID(), targetId, SpaceRole.ADMIN),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verify(spaceMembershipPort, never()).changeRole(any(), any());
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }
}
