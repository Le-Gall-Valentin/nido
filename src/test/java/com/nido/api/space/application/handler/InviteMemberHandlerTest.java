package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.InviteMemberCommand;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.InvitationCodeGeneratorPort;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteMemberHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock InvitationCodeGeneratorPort invitationCodeGeneratorPort;
    @Mock SpaceRepository spaceRepository;
    @Mock SpaceMembershipPort spaceMembershipPort;
    @Mock MemberProfilePort memberProfilePort;

    private InviteMemberHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID inviteeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new InviteMemberHandler(
            spaceInvitationPort, invitationCodeGeneratorPort, spaceRepository, spaceMembershipPort, memberProfilePort);
    }

    @Test
    void an_admin_invites_an_existing_account() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(memberProfilePort.findByEmail("carol@example.com"))
            .thenReturn(Optional.of(new MemberProfile(inviteeId, "carol", "carol@example.com")));
        when(spaceMembershipPort.find(spaceId, inviteeId)).thenReturn(Optional.empty());
        when(invitationCodeGeneratorPort.generate()).thenReturn("NIDO-ABC123");
        Instant beforeCall = Instant.now();
        SpaceInvitation created = new SpaceInvitation(UUID.randomUUID(), spaceId, "carol@example.com",
            SpaceRole.MEMBER, "NIDO-ABC123", com.nido.api.space.domain.model.InvitationStatus.PENDING,
            beforeCall.plus(InviteMemberCommand.VALIDITY), callerId, null, beforeCall);
        when(spaceInvitationPort.create(any(), anyString(), any(), anyString(), any(), any()))
            .thenReturn(created);

        SpaceInvitationView view = handler.invite(
            new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.MEMBER, callerId),
            membership(callerId, SpaceRole.ADMIN));
        Instant afterCall = Instant.now();

        verify(invitationCodeGeneratorPort).generate();
        ArgumentCaptor<Instant> expiresAtCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(spaceInvitationPort).create(eq(spaceId), eq("carol@example.com"), eq(SpaceRole.MEMBER),
            eq("NIDO-ABC123"), expiresAtCaptor.capture(), eq(callerId));
        assertThat(expiresAtCaptor.getValue())
            .isBetween(beforeCall.plus(InviteMemberCommand.VALIDITY), afterCall.plus(InviteMemberCommand.VALIDITY));
        assertThat(view.code()).isEqualTo("NIDO-ABC123");
        assertThat(view.email()).isEqualTo("carol@example.com");
        assertThat(view.role()).isEqualTo(SpaceRole.MEMBER);
        assertThat(view.expiresAt()).isEqualTo(created.expiresAt());
    }

    @Test
    void a_member_cannot_invite() {
        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(spaceInvitationPort, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void a_viewer_cannot_invite() {
        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.VIEWER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verify(spaceInvitationPort, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void a_command_targeting_another_space_than_the_authorized_one_is_refused_before_anything_else() {
        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(UUID.randomUUID(), "carol@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NotAMember.class);
        verifyNoInteractions(spaceRepository, memberProfilePort, spaceInvitationPort, invitationCodeGeneratorPort);
    }

    @Test
    void an_address_without_an_account_is_refused() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(memberProfilePort.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "nobody@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.NoAccountForEmail.class);
        verify(spaceInvitationPort, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void an_address_already_a_member_is_refused() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(sharedSpace()));
        when(memberProfilePort.findByEmail("carol@example.com"))
            .thenReturn(Optional.of(new MemberProfile(inviteeId, "carol", "carol@example.com")));
        when(spaceMembershipPort.find(spaceId, inviteeId))
            .thenReturn(Optional.of(membership(inviteeId, SpaceRole.MEMBER)));

        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.AlreadyMember.class);
        verify(spaceInvitationPort, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void the_personal_space_refuses_every_invitation() {
        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(personalSpace()));

        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.MEMBER, callerId),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.PersonalSpaceImmutable.class);
        verify(spaceInvitationPort, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void the_owner_role_cannot_be_invited() {
        assertThatThrownBy(() -> handler.invite(
                new InviteMemberCommand(spaceId, "carol@example.com", SpaceRole.OWNER, callerId),
                membership(callerId, SpaceRole.OWNER)))
            .isInstanceOf(SpaceException.OwnerRoleNotAssignable.class);
        verifyNoInteractions(spaceInvitationPort);
    }

    private SpaceMembership membership(UUID userId, SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, userId, role, Instant.now());
    }

    private Space sharedSpace() {
        return new Space(spaceId, SpaceType.SHARED, "Chez Valentin", null, "#4a7fa0", "🏠", null, Instant.now());
    }

    private Space personalSpace() {
        return new Space(spaceId, SpaceType.PERSONAL, "Perso", null, "#8a7d6b", "👤", callerId, Instant.now());
    }
}
