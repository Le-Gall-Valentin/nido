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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
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

        handler.handleUserDeletion(userId, "alice");

        verify(spaceCommandPort).delete(personalSpaceId);
    }

    @Test
    void a_plain_membership_is_removed_and_the_others_hear_the_member_left_nido() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.MEMBER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));

        handler.handleUserDeletion(userId, "alice");

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
        verify(spaceNotifier).memberLeftNido(sharedSpaceId, userId, "alice");
    }

    @Test
    void ownership_passes_to_the_successor() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        SpaceMembership successor = new SpaceMembership(
            UUID.randomUUID(), sharedSpaceId, UUID.randomUUID(), SpaceRole.ADMIN, Instant.now());
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.of(successor));

        handler.handleUserDeletion(userId, "alice");

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceMembershipPort).changeRole(successor.id(), SpaceRole.OWNER);
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
        verify(spaceNotifier).ownershipInherited(sharedSpaceId, userId, "alice", successor.userId());
    }

    @Test
    void a_space_without_heir_tells_its_invitees_before_it_goes() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findByIds(List.of(sharedSpaceId))).thenReturn(List.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.empty());

        handler.handleUserDeletion(userId, "alice");

        InOrder order = inOrder(spaceNotifier, spaceCommandPort);
        order.verify(spaceNotifier).spaceDeletedWithoutHeir(sharedSpaceId, userId, "alice");
        order.verify(spaceCommandPort).delete(sharedSpaceId);
    }

    @Test
    void invitations_for_the_deleted_account_are_removed() {
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of());

        handler.handleUserDeletion(userId, "alice");

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
