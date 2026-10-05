package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationDelivery;
import com.nido.api.authentication.application.service.InvitationIssuer;
import com.nido.api.authentication.domain.model.AccountContact;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.model.AuthenticationException;
import com.nido.api.authentication.domain.model.UserProfile;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import com.nido.api.authentication.domain.port.out.UserProfilePort;
import com.nido.api.shared.model.Role;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InviteAccountHandlerTest {

    @Mock UserProfilePort profiles;
    @Mock AccountInvitationRepository invitations;
    @Mock InvitationIssuer issuer;

    private final UserProfile carol = new UserProfile(UUID.randomUUID(), "carol", "carol@test.com", true, Role.USER,
        Instant.parse("2026-10-05T10:00:00Z"), null);

    @Test
    void the_account_is_invited_under_its_own_name_and_address() {
        when(profiles.findById(carol.id())).thenReturn(Optional.of(carol));
        when(issuer.invite(AccountContact.of(carol), "bob")).thenReturn(new InvitationDelivery.Mailed());

        assertThat(new InviteAccountHandler(profiles, invitations, issuer).invite(carol.id(), "bob"))
            .isEqualTo(new InvitationDelivery.Mailed());
    }

    @Test
    void an_account_that_does_not_exist_is_not_invited() {
        when(profiles.findById(carol.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new InviteAccountHandler(profiles, invitations, issuer).invite(carol.id(), "bob"))
            .isInstanceOf(AuthenticationException.UserNotFound.class);
        verifyNoInteractions(issuer);
    }

    @Test
    void asked_again_the_invitation_is_looked_at_under_lock_then_replaced() {
        when(invitations.lockForUser(carol.id())).thenReturn(Optional.of(
            new AccountInvitation(carol.id(), Instant.parse("2026-10-05T10:00:00Z"), Instant.parse("2026-10-12T10:00:00Z"))));
        when(profiles.findById(carol.id())).thenReturn(Optional.of(carol));
        when(issuer.invite(AccountContact.of(carol), "bob")).thenReturn(new InvitationDelivery.Mailed());

        assertThat(new InviteAccountHandler(profiles, invitations, issuer).inviteAgain(carol.id(), "bob"))
            .contains(new InvitationDelivery.Mailed());
        InOrder order = inOrder(invitations, issuer);
        order.verify(invitations).lockForUser(carol.id());
        order.verify(issuer).invite(AccountContact.of(carol), "bob");
    }

    @Test
    void asked_again_once_the_account_has_chosen_its_password_nothing_is_issued() {
        when(invitations.lockForUser(carol.id())).thenReturn(Optional.empty());

        assertThat(new InviteAccountHandler(profiles, invitations, issuer).inviteAgain(carol.id(), "bob")).isEmpty();
        verifyNoInteractions(issuer);
    }
}
