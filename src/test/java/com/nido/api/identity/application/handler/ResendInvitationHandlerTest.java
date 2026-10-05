package com.nido.api.identity.application.handler;

import com.nido.api.identity.domain.model.IdentityException;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.ResendInvitationCommand;
import com.nido.api.identity.domain.model.User;
import com.nido.api.identity.domain.port.out.AccountInvitationPort;
import com.nido.api.identity.domain.port.out.UserRepository;
import com.nido.api.shared.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResendInvitationHandlerTest {

    @Mock UserRepository userRepository;
    @Mock AccountInvitationPort invitations;

    private final UUID callerId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private ResendInvitationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ResendInvitationHandler(userRepository, invitations);
        when(userRepository.findById(callerId))
            .thenReturn(Optional.of(new User(callerId, "bob", "bob@test.com", Role.ADMIN, true, Instant.now(), null)));
        when(invitations.isInvited(targetId)).thenReturn(true);
        when(invitations.invite(targetId, "bob")).thenReturn(new InvitationDelivery.Mailed());
    }

    private void target(Role role, boolean active) {
        when(userRepository.findById(targetId))
            .thenReturn(Optional.of(new User(targetId, "carol", "carol@test.com", role, active, Instant.now(), null)));
    }

    private InvitationDelivery resend(Role callerRole) {
        return handler.resend(new ResendInvitationCommand(targetId, callerId, callerRole));
    }

    @Test
    void an_invited_account_gets_a_new_link_from_the_admin_who_asked() {
        target(Role.USER, true);

        assertThat(resend(Role.ADMIN)).isEqualTo(new InvitationDelivery.Mailed());
        verify(invitations).invite(targetId, "bob");
    }

    @Test
    void an_account_that_chose_its_password_has_nothing_to_resend() {
        target(Role.USER, true);
        when(invitations.isInvited(targetId)).thenReturn(false);

        assertThatThrownBy(() -> resend(Role.ADMIN)).isInstanceOf(IdentityException.AccountAlreadyJoined.class);
        verify(invitations, never()).invite(any(), any());
    }

    @Test
    void a_deactivated_account_is_not_invited_again() {
        target(Role.USER, false);

        assertThatThrownBy(() -> resend(Role.ADMIN)).isInstanceOf(IdentityException.UserNotActive.class);
        verify(invitations, never()).invite(any(), any());
    }

    @Test
    void an_admin_cannot_invite_an_admin_again() {
        target(Role.ADMIN, true);

        assertThatThrownBy(() -> resend(Role.ADMIN)).isInstanceOf(IdentityException.InsufficientPermissions.class);
    }

    @Test
    void nobody_resends_their_own_invitation() {
        assertThatThrownBy(() -> handler.resend(new ResendInvitationCommand(callerId, callerId, Role.ADMIN)))
            .isInstanceOf(IdentityException.InsufficientPermissions.class);
    }

    @Test
    void an_unknown_account_is_not_found() {
        when(userRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resend(Role.ADMIN)).isInstanceOf(IdentityException.UserNotFound.class);
    }
}
