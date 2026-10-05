package com.nido.api.identity.infrastructure.security;

import com.nido.api.authentication.application.dto.InvitationStatus;
import com.nido.api.authentication.application.port.in.AccountInvitationStatusQuery;
import com.nido.api.authentication.application.port.in.InviteAccountUseCase;
import com.nido.api.identity.domain.model.InvitationDelivery;
import com.nido.api.identity.domain.model.InvitationState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountInvitationAdapterTest {

    @Mock InviteAccountUseCase inviteAccount;
    @Mock AccountInvitationStatusQuery invitationStatus;

    private final UUID carol = UUID.randomUUID();

    private AccountInvitationAdapter adapter() {
        return new AccountInvitationAdapter(inviteAccount, invitationStatus);
    }

    @Test
    void how_the_invitation_left_is_told_in_identity_s_words() {
        when(inviteAccount.invite(carol, "bob"))
            .thenReturn(new com.nido.api.authentication.application.dto.InvitationDelivery.Mailed())
            .thenReturn(new com.nido.api.authentication.application.dto.InvitationDelivery.Link("/welcome#token=x"));

        assertThat(adapter().invite(carol, "bob")).isEqualTo(new InvitationDelivery.Mailed());
        assertThat(adapter().invite(carol, "bob")).isEqualTo(new InvitationDelivery.Link("/welcome#token=x"));
    }

    @Test
    void the_state_of_each_invitation_is_carried_over() {
        Instant expiresAt = Instant.parse("2026-10-12T10:00:00Z");
        when(invitationStatus.statusAmong(List.of(carol))).thenReturn(Map.of(carol, new InvitationStatus(true, expiresAt)));

        assertThat(adapter().invitationsAmong(List.of(carol))).isEqualTo(Map.of(carol, new InvitationState(true, expiresAt)));
    }
}
