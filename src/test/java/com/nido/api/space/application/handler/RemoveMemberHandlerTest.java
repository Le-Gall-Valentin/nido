package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.RemoveMemberCommand;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveMemberHandlerTest {

    @Mock SpaceMembershipPort spaceMembershipPort;

    private RemoveMemberHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RemoveMemberHandler(spaceMembershipPort);
    }

    @Test
    void an_admin_can_remove_a_member() {
        SpaceMembership target = membership(targetId, SpaceRole.MEMBER);
        when(spaceMembershipPort.find(spaceId, targetId)).thenReturn(Optional.of(target));

        handler.remove(new RemoveMemberCommand(spaceId, targetId), membership(callerId, SpaceRole.ADMIN));

        verify(spaceMembershipPort).remove(target.id());
    }

    @Test
    void a_viewer_cannot_remove_anyone() {
        assertThatThrownBy(() -> handler.remove(
                new RemoveMemberCommand(spaceId, targetId), membership(callerId, SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void nobody_removes_themselves_here() {
        assertThatThrownBy(() -> handler.remove(
                new RemoveMemberCommand(spaceId, callerId), membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.SelfManagementForbidden.class);
    }

    @Test
    void the_owner_cannot_be_removed() {
        when(spaceMembershipPort.find(spaceId, targetId))
            .thenReturn(Optional.of(membership(targetId, SpaceRole.OWNER)));

        assertThatThrownBy(() -> handler.remove(
                new RemoveMemberCommand(spaceId, targetId), membership(callerId, SpaceRole.ADMIN)))
            .isInstanceOf(SpaceException.OwnerMembershipProtected.class);
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }
}
