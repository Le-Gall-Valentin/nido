package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.AcceptInvitationCommand;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptInvitationHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock SpaceRepository spaceRepository;
    @Mock SpaceMembershipPort spaceMembershipPort;

    private AcceptInvitationHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String userEmail = "carol@example.com";

    @BeforeEach
    void setUp() {
        handler = new AcceptInvitationHandler(spaceInvitationPort, spaceRepository, spaceMembershipPort);
    }

    @Test
    void a_pending_invitation_addressed_to_the_caller_is_accepted_and_grants_its_own_role() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(spaceMembershipPort.find(spaceId, userId)).thenReturn(Optional.empty());
        when(spaceInvitationPort.claim(eq(invitationId), any())).thenReturn(true);

        UUID joined = handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail);

        assertThat(joined).isEqualTo(spaceId);
        InOrder order = inOrder(spaceInvitationPort, spaceMembershipPort);
        order.verify(spaceInvitationPort).claim(eq(invitationId), any());
        // La réclamation précède la création de l'adhésion : c'est le point de sérialisation.
        order.verify(spaceMembershipPort).add(spaceId, userId, SpaceRole.ADMIN);
    }

    @Test
    void an_unknown_code_is_not_found() {
        when(spaceInvitationPort.findByCode("NIDO-UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-UNKNOWN"), userId, userEmail))
            .isInstanceOf(SpaceException.InvitationNotFound.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    // Épingle SpaceInvitation.ensureAddressedTo : c'est ce contrôle, et lui seul, qui rend
    // légitime le stockage du code en clair. Le relâcher transformerait le code en jeton
    // porteur — un lien d'invitation ouvert — sans que rien d'autre ne le signale.
    @Test
    void a_code_cannot_be_used_by_someone_else_which_is_what_makes_clear_text_storage_safe() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, "eve@example.com"))
            .isInstanceOf(SpaceException.InvitationEmailMismatch.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
        verify(spaceInvitationPort, never()).claim(any(), any());
    }

    @Test
    void an_expired_invitation_is_refused() {
        SpaceInvitation expired = new SpaceInvitation(invitationId, spaceId, userEmail, SpaceRole.ADMIN,
            "NIDO-ABC123", InvitationStatus.PENDING, Instant.now().minusSeconds(60), UUID.randomUUID(), null, Instant.now());
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail))
            .isInstanceOf(SpaceException.InvitationExpired.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    @Test
    void an_already_settled_invitation_is_refused() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.ACCEPTED)));

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail))
            .isInstanceOf(SpaceException.InvitationNotPending.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    @Test
    void a_caller_already_a_member_of_the_space_cannot_accept_again() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(spaceMembershipPort.find(spaceId, userId))
            .thenReturn(Optional.of(new SpaceMembership(UUID.randomUUID(), spaceId, userId, SpaceRole.MEMBER, Instant.now())));

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail))
            .isInstanceOf(SpaceException.AlreadyMember.class);
        verify(spaceInvitationPort, never()).claim(any(), any());
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    @Test
    void a_vanished_space_is_reported_as_not_found() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail))
            .isInstanceOf(SpaceException.SpaceNotFound.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    // Ce cas ne peut être produit qu'en simulant la course : claim rend false parce qu'une
    // autre requête a gagné entre la lecture et la réclamation.
    @Test
    void a_claim_lost_to_a_concurrent_acceptance_aborts_before_creating_a_membership() {
        when(spaceInvitationPort.findByCode("NIDO-ABC123")).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(spaceMembershipPort.find(spaceId, userId)).thenReturn(Optional.empty());
        when(spaceInvitationPort.claim(eq(invitationId), any())).thenReturn(false);

        assertThatThrownBy(() -> handler.accept(new AcceptInvitationCommand("NIDO-ABC123"), userId, userEmail))
            .isInstanceOf(SpaceException.InvitationNotPending.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    @Test
    void a_pending_invitation_addressed_to_the_caller_is_accepted_by_id_and_grants_its_own_role() {
        when(spaceInvitationPort.findById(invitationId)).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(spaceMembershipPort.find(spaceId, userId)).thenReturn(Optional.empty());
        when(spaceInvitationPort.claim(eq(invitationId), any())).thenReturn(true);

        UUID joined = handler.acceptById(invitationId, userId, userEmail);

        assertThat(joined).isEqualTo(spaceId);
        InOrder order = inOrder(spaceInvitationPort, spaceMembershipPort);
        order.verify(spaceInvitationPort).claim(eq(invitationId), any());
        // La réclamation précède la création de l'adhésion : c'est le point de sérialisation.
        order.verify(spaceMembershipPort).add(spaceId, userId, SpaceRole.ADMIN);
    }

    @Test
    void an_unknown_id_is_not_found() {
        UUID unknownId = UUID.randomUUID();
        when(spaceInvitationPort.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.acceptById(unknownId, userId, userEmail))
            .isInstanceOf(SpaceException.InvitationNotFound.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
    }

    // Épingle SpaceInvitation.ensureAddressedTo : c'est ce contrôle, et lui seul, qui rend
    // légitime le stockage du code en clair. Le relâcher transformerait l'identifiant en jeton
    // porteur — n'importe qui devinant un id pourrait rejoindre — sans que rien d'autre ne le signale.
    @Test
    void an_invitation_cannot_be_accepted_by_id_by_someone_else_which_is_what_makes_clear_text_storage_safe() {
        when(spaceInvitationPort.findById(invitationId)).thenReturn(Optional.of(invitation(InvitationStatus.PENDING)));

        assertThatThrownBy(() -> handler.acceptById(invitationId, userId, "eve@example.com"))
            .isInstanceOf(SpaceException.InvitationEmailMismatch.class);
        verify(spaceMembershipPort, never()).add(any(), any(), any());
        verify(spaceInvitationPort, never()).claim(any(), any());
    }

    private SpaceInvitation invitation(InvitationStatus status) {
        return new SpaceInvitation(invitationId, spaceId, userEmail, SpaceRole.ADMIN, "NIDO-ABC123", status,
            Instant.now().plusSeconds(3600), UUID.randomUUID(), status == InvitationStatus.ACCEPTED ? Instant.now() : null,
            Instant.now());
    }

    private Space sharedSpace() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#4a7fa0", "🏠", null, Instant.now());
    }
}
