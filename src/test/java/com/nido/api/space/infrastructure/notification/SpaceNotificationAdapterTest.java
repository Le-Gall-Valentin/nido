package com.nido.api.space.infrastructure.notification;

import com.nido.api.mail.domain.model.AppPath;
import com.nido.api.notifications.application.port.in.NotifyUseCase;
import com.nido.api.notifications.domain.model.NotificationRequest;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpaceNotificationAdapterTest {

    @Mock NotifyUseCase notify;

    private final UUID spaceId = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private final AppPath members = new AppPath("/s/00000000-0000-0000-0000-000000000042/members");
    private final AppPath spaces = new AppPath("/spaces");
    private final Addressee bob = new Addressee(UUID.randomUUID(), "bob");
    private final Addressee carol = new Addressee(UUID.randomUUID(), "carol");

    private SpaceNotificationAdapter adapter() {
        return new SpaceNotificationAdapter(notify);
    }

    private List<NotificationRequest> sent(int count) {
        ArgumentCaptor<NotificationRequest> requests = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notify, times(count)).notify(requests.capture());
        return requests.getAllValues();
    }

    @Test
    void the_invitee_is_told_who_invites_where_and_the_mail_dies_with_the_invitation() {
        UUID inviteeId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-10-09T10:00:00Z");
        SpaceInvitation invitation = new SpaceInvitation(UUID.randomUUID(), UUID.randomUUID(), inviteeId,
            SpaceRole.MEMBER, "NIDO-ABC123", InvitationStatus.PENDING, expiresAt, UUID.randomUUID(), null,
            Instant.parse("2026-10-02T10:00:00Z"));

        adapter().invitationIssued(invitation, "carol", "alice", "Chez nous");

        NotificationRequest request = sent(1).getFirst();
        assertThat(request.recipientId()).isEqualTo(inviteeId);
        assertThat(request.expiresAt()).isEqualTo(expiresAt);
        assertThat(request.notification()).isEqualTo(
            new SpaceInvitationNotification("carol", "alice", "Chez nous", 7, spaces));
    }

    @Test
    void each_member_gets_their_own_mail_greeting_them() {
        adapter().memberJoined(spaceId, "Chez nous", "dave", List.of(bob, carol));

        List<NotificationRequest> requests = sent(2);
        assertThat(requests).extracting(NotificationRequest::recipientId).containsExactly(bob.userId(), carol.userId());
        assertThat(requests).extracting(NotificationRequest::notification).containsExactly(
            new MemberJoinedNotification("bob", "dave", "Chez nous", members),
            new MemberJoinedNotification("carol", "dave", "Chez nous", members));
        assertThat(requests).extracting(NotificationRequest::expiresAt).containsOnlyNulls();
    }

    @Test
    void a_departure() {
        adapter().memberLeft(spaceId, "Chez nous", "dave", List.of(bob));

        assertThat(sent(1).getFirst().notification()).isEqualTo(new MemberLeftNotification("bob", "dave", "Chez nous", false, members));
    }

    @Test
    void a_removal_told_to_the_others_and_to_the_removed() {
        adapter().memberRemoved(spaceId, "Chez nous", "alice", "dave", List.of(bob));
        adapter().removedFromSpace("Chez nous", "alice", carol);

        List<NotificationRequest> requests = sent(2);
        assertThat(requests.get(0).notification()).isEqualTo(new MemberRemovedNotification("bob", "alice", "dave", "Chez nous", members));
        assertThat(requests.get(1).recipientId()).isEqualTo(carol.userId());
        assertThat(requests.get(1).notification()).isEqualTo(new RemovedFromSpaceNotification("carol", "alice", "Chez nous", spaces));
    }

    @Test
    void a_deletion_points_to_the_spaces_that_remain() {
        adapter().spaceDeleted("Chez nous", "alice", List.of(bob));

        assertThat(sent(1).getFirst().notification()).isEqualTo(new SpaceDeletedNotification("bob", "alice", "Chez nous", spaces));
    }

    @Test
    void a_role_change() {
        adapter().roleChanged(spaceId, "Chez nous", "alice", bob, SpaceRole.VIEWER, SpaceRole.MEMBER);

        NotificationRequest request = sent(1).getFirst();
        assertThat(request.recipientId()).isEqualTo(bob.userId());
        assertThat(request.notification()).isEqualTo(
            new RoleChangedNotification("bob", "alice", "Chez nous", SpaceRole.VIEWER, SpaceRole.MEMBER, members));
    }

    @Test
    void a_new_owner_and_the_others_after_a_transfer() {
        adapter().ownershipReceived(spaceId, "Chez nous", "alice", bob);
        adapter().ownerChanged(spaceId, "Chez nous", "alice", "bob", List.of(carol));

        assertThat(sent(2)).extracting(NotificationRequest::notification).containsExactly(
            new OwnershipReceivedNotification("bob", "alice", "Chez nous", false, members),
            new OwnerChangedNotification("carol", "alice", "bob", "Chez nous", false, members));
    }

    @Test
    void a_member_who_left_nido_is_told_to_the_others() {
        adapter().memberLeftNido(spaceId, "Chez nous", "dave", List.of(bob));

        assertThat(sent(1).getFirst().notification())
            .isEqualTo(new MemberLeftNotification("bob", "dave", "Chez nous", true, members));
    }

    @Test
    void a_succession_names_the_former_owner_to_the_heir_and_the_others() {
        adapter().ownershipInherited(spaceId, "Chez nous", "alice", bob);
        adapter().ownerSucceeded(spaceId, "Chez nous", "alice", "bob", List.of(carol));

        assertThat(sent(2)).extracting(NotificationRequest::notification).containsExactly(
            new OwnershipReceivedNotification("bob", "alice", "Chez nous", true, members),
            new OwnerChangedNotification("carol", "alice", "bob", "Chez nous", true, members));
    }
}
