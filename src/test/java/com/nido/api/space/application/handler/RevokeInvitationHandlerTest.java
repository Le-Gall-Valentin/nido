package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeInvitationHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;

    private RevokeInvitationHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new RevokeInvitationHandler(spaceInvitationPort);
    }

    @Test
    void an_admin_revokes_a_pending_invitation() {
        when(spaceInvitationPort.findById(invitationId)).thenReturn(Optional.of(invitation(spaceId, InvitationStatus.PENDING)));

        handler.revoke(spaceId, invitationId, membership(SpaceRole.ADMIN));

        verify(spaceInvitationPort).revoke(invitationId);
    }

    @Test
    void a_member_cannot_revoke() {
        assertThatThrownBy(() -> handler.revoke(spaceId, invitationId, membership(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(spaceInvitationPort, never()).revoke(invitationId);
    }

    @Test
    void an_unknown_invitation_is_not_found() {
        when(spaceInvitationPort.findById(invitationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.revoke(spaceId, invitationId, membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.InvitationNotFound.class);
        verify(spaceInvitationPort, never()).revoke(invitationId);
    }

    @Test
    void an_invitation_belonging_to_another_space_is_treated_as_missing() {
        UUID otherSpaceId = UUID.randomUUID();
        when(spaceInvitationPort.findById(invitationId))
            .thenReturn(Optional.of(invitation(otherSpaceId, InvitationStatus.PENDING)));

        assertThatThrownBy(() -> handler.revoke(spaceId, invitationId, membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.InvitationNotFound.class);
        verify(spaceInvitationPort, never()).revoke(invitationId);
    }

    @Test
    void an_already_settled_invitation_cannot_be_revoked_again() {
        when(spaceInvitationPort.findById(invitationId))
            .thenReturn(Optional.of(invitation(spaceId, InvitationStatus.ACCEPTED)));

        assertThatThrownBy(() -> handler.revoke(spaceId, invitationId, membership(SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.InvitationNotPending.class);
        verify(spaceInvitationPort, never()).revoke(invitationId);
    }

    private SpaceMembership membership(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, callerId, role, Instant.now());
    }

    private SpaceInvitation invitation(UUID ownerSpaceId, InvitationStatus status) {
        return new SpaceInvitation(invitationId, ownerSpaceId, "carol@example.com", SpaceRole.MEMBER,
            "NIDO-ABC123", status, Instant.now().plusSeconds(3600), callerId, null, Instant.now());
    }
}
