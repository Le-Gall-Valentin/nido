package com.nido.api.authentication.application.handler;

import com.nido.api.authentication.application.dto.InvitationStatus;
import com.nido.api.authentication.domain.model.AccountInvitation;
import com.nido.api.authentication.domain.port.out.AccountInvitationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountInvitationStatusHandlerTest {

    private static final Instant NOW = Instant.parse("2026-10-05T10:00:00Z");

    @Mock AccountInvitationRepository invitations;

    private final UUID carol = UUID.randomUUID();
    private final UUID dave = UUID.randomUUID();

    private AccountInvitationStatusHandler handler() {
        return new AccountInvitationStatusHandler(invitations, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void an_account_with_an_invitation_is_invited_even_once_it_expired() {
        when(invitations.findByUserId(carol)).thenReturn(Optional.of(new AccountInvitation(carol, NOW.minusSeconds(9), NOW)));
        when(invitations.findByUserId(dave)).thenReturn(Optional.empty());

        assertThat(handler().isInvited(carol)).isTrue();
        assertThat(handler().isInvited(dave)).isFalse();
    }

    @Test
    void the_status_says_whether_each_invitation_expired() {
        Instant later = NOW.plusSeconds(3600);
        when(invitations.findByUserIds(List.of(carol, dave))).thenReturn(Map.of(
            carol, new AccountInvitation(carol, NOW.minusSeconds(60), later),
            dave, new AccountInvitation(dave, NOW.minusSeconds(60), NOW)));

        assertThat(handler().statusAmong(List.of(carol, dave))).isEqualTo(Map.of(
            carol, new InvitationStatus(false, later),
            dave, new InvitationStatus(true, NOW)));
    }
}
