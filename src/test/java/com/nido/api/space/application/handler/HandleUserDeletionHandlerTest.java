package com.nido.api.space.application.handler;

import com.nido.api.space.application.service.SpaceNotifier;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceAppearance;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceCommandPort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleUserDeletionHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;
    @Mock SpaceMembershipPort spaceMembershipPort;
    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock SpaceNotifier spaceNotifier;

    private HandleUserDeletionHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final UUID personalSpaceId = UUID.randomUUID();
    private final UUID sharedSpaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new HandleUserDeletionHandler(spaceRepository, spaceCommandPort, spaceMembershipPort, spaceInvitationPort, spaceNotifier);
    }

    @Test
    void the_personal_space_is_deleted() {
        SpaceMembership membership = membership(personalSpaceId, SpaceRole.OWNER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(personalSpaceId))).thenReturn(List.of(personal()));

        handler.handleUserDeletion(userId);

        verify(spaceCommandPort).delete(personalSpaceId);
    }

    @Test
    void a_plain_membership_is_simply_removed() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.MEMBER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));

        handler.handleUserDeletion(userId);

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
        verifyNoInteractions(spaceNotifier);
    }

    @Test
    void ownership_passes_to_the_successor() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        SpaceMembership successor = new SpaceMembership(
            UUID.randomUUID(), sharedSpaceId, UUID.randomUUID(), SpaceRole.ADMIN, Instant.now());
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.of(successor));

        handler.handleUserDeletion(userId);

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceMembershipPort).changeRole(successor.id(), SpaceRole.OWNER);
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
        verify(spaceNotifier).ownershipInherited(sharedSpaceId, successor.userId());
    }

    @Test
    void a_space_with_no_successor_is_deleted() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.empty());

        handler.handleUserDeletion(userId);

        verify(spaceCommandPort).delete(sharedSpaceId);
        verifyNoInteractions(spaceNotifier);
    }

    @Test
    void invitations_for_the_deleted_account_are_removed() {
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of());

        handler.handleUserDeletion(userId);

        verify(spaceInvitationPort).deleteAllForInvitee(userId);
    }

    private SpaceMembership membership(UUID spaceId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space personal() {
        return new Space(personalSpaceId, SpaceType.PERSONAL, "Perso", null, SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, userId, ZoneId.of("Europe/Paris"), Instant.now());
    }

    private Space shared() {
        return new Space(sharedSpaceId, SpaceType.SHARED, "Chez Valentin", null, "#c17a5c", "🏡", null, ZoneId.of("Europe/Paris"), Instant.now());
    }
}
