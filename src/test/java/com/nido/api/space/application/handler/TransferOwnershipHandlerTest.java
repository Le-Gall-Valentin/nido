package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.model.TransferOwnershipCommand;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferOwnershipHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceMembershipPort spaceMembershipPort;

    private TransferOwnershipHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new TransferOwnershipHandler(spaceRepository, spaceMembershipPort);
    }

    @Test
    void demotes_the_current_owner_before_promoting_the_new_one() {
        SpaceMembership caller = membership(ownerId, SpaceRole.OWNER);
        SpaceMembership target = membership(targetId, SpaceRole.ADMIN);
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));
        when(spaceMembershipPort.find(spaceId, targetId)).thenReturn(Optional.of(target));

        handler.transfer(new TransferOwnershipCommand(spaceId, targetId), caller);

        // l'ordre compte : l'index unique partiel n'autorise qu'un OWNER à la fois
        InOrder order = inOrder(spaceMembershipPort);
        order.verify(spaceMembershipPort).changeRole(caller.id(), SpaceRole.ADMIN);
        order.verify(spaceMembershipPort).changeRole(target.id(), SpaceRole.OWNER);
    }

    @Test
    void only_the_owner_can_transfer() {
        assertThatThrownBy(() -> handler.transfer(
                new TransferOwnershipCommand(spaceId, targetId), membership(ownerId, SpaceRole.ADMIN)))
            .isInstanceOf(SpaceException.OwnerRequired.class);
    }

    @Test
    void a_viewer_cannot_become_owner() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(shared()));
        when(spaceMembershipPort.find(spaceId, targetId))
            .thenReturn(Optional.of(membership(targetId, SpaceRole.VIEWER)));

        assertThatThrownBy(() -> handler.transfer(
                new TransferOwnershipCommand(spaceId, targetId), membership(ownerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
    }

    @Test
    void transferring_to_oneself_is_refused() {
        assertThatThrownBy(() -> handler.transfer(
                new TransferOwnershipCommand(spaceId, ownerId), membership(ownerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.SelfManagementForbidden.class);
    }

    @Test
    void a_command_targeting_another_space_than_the_authorized_one_is_refused() {
        assertThatThrownBy(() -> handler.transfer(
                new TransferOwnershipCommand(UUID.randomUUID(), targetId), membership(ownerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verifyNoInteractions(spaceRepository, spaceMembershipPort);
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space shared() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏡", null, Instant.now());
    }
}
