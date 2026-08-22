package com.nido.api.space.application.handler;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleUserDeletionHandlerTest {

    @Mock SpaceRepository spaceRepository;
    @Mock SpaceCommandPort spaceCommandPort;
    @Mock SpaceMembershipPort spaceMembershipPort;
    @Mock SpaceInvitationPort spaceInvitationPort;

    private HandleUserDeletionHandler handler;

    private final UUID userId = UUID.randomUUID();
    private final UUID personalSpaceId = UUID.randomUUID();
    private final UUID sharedSpaceId = UUID.randomUUID();
    private final String userEmail = "deleted@test.com";

    @BeforeEach
    void setUp() {
        handler = new HandleUserDeletionHandler(spaceRepository, spaceCommandPort, spaceMembershipPort, spaceInvitationPort);
    }

    @Test
    void the_personal_space_is_deleted() {
        SpaceMembership membership = membership(personalSpaceId, SpaceRole.OWNER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findById(personalSpaceId)).thenReturn(Optional.of(personal()));

        handler.handleUserDeletion(userId, userEmail);

        verify(spaceCommandPort).delete(personalSpaceId);
    }

    @Test
    void a_plain_membership_is_simply_removed() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.MEMBER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findById(sharedSpaceId)).thenReturn(Optional.of(shared()));

        handler.handleUserDeletion(userId, userEmail);

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
    }

    @Test
    void ownership_passes_to_the_successor() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        SpaceMembership successor = new SpaceMembership(
            UUID.randomUUID(), sharedSpaceId, UUID.randomUUID(), SpaceRole.ADMIN, Instant.now());
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findById(sharedSpaceId)).thenReturn(Optional.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.of(successor));

        handler.handleUserDeletion(userId, userEmail);

        verify(spaceMembershipPort).remove(membership.id());
        verify(spaceMembershipPort).changeRole(successor.id(), SpaceRole.OWNER);
        verify(spaceCommandPort, never()).delete(sharedSpaceId);
    }

    @Test
    void a_space_with_no_successor_is_deleted() {
        SpaceMembership membership = membership(sharedSpaceId, SpaceRole.OWNER);
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of(membership));
        when(spaceRepository.findById(sharedSpaceId)).thenReturn(Optional.of(shared()));
        when(spaceMembershipPort.findSuccessor(sharedSpaceId, userId)).thenReturn(Optional.empty());

        handler.handleUserDeletion(userId, userEmail);

        verify(spaceCommandPort).delete(sharedSpaceId);
    }

    @Test
    void invitations_addressed_to_the_deleted_user_are_removed() {
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of());

        handler.handleUserDeletion(userId, userEmail);

        verify(spaceInvitationPort).deleteAllForEmail(userEmail);
    }

    @Test
    void a_null_email_skips_invitation_cleanup_without_throwing() {
        when(spaceMembershipPort.findByUser(userId)).thenReturn(List.of());

        handler.handleUserDeletion(userId, null);

        verify(spaceInvitationPort, never()).deleteAllForEmail(any());
    }

    private SpaceMembership membership(UUID spaceId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space personal() {
        return new Space(personalSpaceId, SpaceType.PERSONAL, "Perso", null,
            SpaceAppearance.PERSONAL_ACCENT, SpaceAppearance.PERSONAL_GLYPH, userId, Instant.now());
    }

    private Space shared() {
        return new Space(sharedSpaceId, SpaceType.SHARED, "Chez Valentin", null,
            "#c17a5c", "🏡", null, Instant.now());
    }
}
