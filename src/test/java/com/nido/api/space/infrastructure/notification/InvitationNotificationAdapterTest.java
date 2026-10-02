package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationNotificationAdapterTest {

    @Mock NotifyUseCase notify;

    @Test
    void the_invitee_is_told_who_invites_where_and_the_mail_dies_with_the_invitation() {
        UUID inviteeId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-10-09T10:00:00Z");
        SpaceInvitation invitation = new SpaceInvitation(UUID.randomUUID(), UUID.randomUUID(), inviteeId,
            SpaceRole.MEMBER, "NIDO-ABC123", InvitationStatus.PENDING, expiresAt, UUID.randomUUID(), null,
            Instant.parse("2026-10-02T10:00:00Z"));

        new InvitationNotificationAdapter(notify).invitationIssued(invitation, "carol", "alice", "Chez nous");

        ArgumentCaptor<NotificationRequest> request = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notify).notify(request.capture());
        assertThat(request.getValue().recipientId()).isEqualTo(inviteeId);
        assertThat(request.getValue().expiresAt()).isEqualTo(expiresAt);
        assertThat(request.getValue().notification()).isEqualTo(
            new SpaceInvitationNotification("carol", "alice", "Chez nous", 7, new AppPath("/spaces")));
    }
}
