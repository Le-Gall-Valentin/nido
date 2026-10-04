package com.nido.api.space.application.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nido.api.space.domain.model.Addressee;
import com.nido.api.space.domain.model.InvitationCancellation;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceMembershipPort;
import com.nido.api.space.domain.port.out.SpaceNotificationPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceNotifierTest {

    @Mock SpaceRepository spaces;
    @Mock SpaceMembershipPort memberships;
    @Mock MemberProfilePort profiles;
    @Mock SpaceNotificationPort notifications;
    @Mock SpaceInvitationPort invitations;

    private final Instant now = Instant.parse("2026-10-04T10:00:00Z");
    private final UUID spaceId = UUID.randomUUID();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();
    private final UUID dave = UUID.randomUUID();
    /** The directory of names; an account missing here can no longer be named. */
    private final Map<UUID, String> names = new HashMap<>();
    private final ListAppender<ILoggingEvent> logged = new ListAppender<>();

    private SpaceNotifier notifier;

    @BeforeEach
    void setUp() {
        logged.start();
        ((Logger) LoggerFactory.getLogger(SpaceNotifier.class)).addAppender(logged);
        names.putAll(Map.of(alice, "alice", bob, "bob", carol, "carol", dave, "dave"));
        lenient().when(profiles.findByIds(any())).thenAnswer(call -> ((Collection<?>) call.getArgument(0)).stream()
            .map(UUID.class::cast).filter(names::containsKey)
            .map(id -> new MemberProfile(id, names.get(id), names.get(id) + "@test.local")).toList());
        lenient().when(spaces.findById(spaceId)).thenReturn(Optional.of(new Space(spaceId, SpaceType.SHARED,
            "Chez nous", null, "#c17a5c", "🏡", null, ZoneId.of("Europe/Paris"), Instant.now())));
        notifier = new SpaceNotifier(spaces, memberships, invitations, new MemberNames(profiles), notifications,
            Clock.fixed(now, ZoneOffset.UTC));
    }

    @AfterEach
    void stopListening() {
        ((Logger) LoggerFactory.getLogger(SpaceNotifier.class)).detachAppender(logged);
    }

    private void membersAre(UUID... userIds) {
        when(memberships.findMemberships(spaceId)).thenReturn(Arrays.stream(userIds)
            .map(id -> new SpaceMembership(UUID.randomUUID(), spaceId, id, SpaceRole.MEMBER, Instant.now())).toList());
    }

    private static Addressee to(UUID id, String name) {
        return new Addressee(id, name);
    }

    private SpaceInvitation invitationFor(UUID invitee, UUID inviter) {
        return new SpaceInvitation(UUID.randomUUID(), spaceId, invitee, SpaceRole.MEMBER, "NIDO-ABC123",
            InvitationStatus.PENDING, Instant.now().plusSeconds(60), inviter, null, Instant.now());
    }

    @Test
    void an_invitee_is_told_by_the_name_of_who_invites() {
        SpaceInvitation invitation = invitationFor(carol, alice);

        notifier.invitationIssued(invitation, "carol", alice);

        verify(notifications).invitationIssued(invitation, "carol", "alice", "Chez nous");
    }

    @Test
    void an_inviter_who_can_no_longer_be_named_tells_nobody() {
        names.remove(alice);

        notifier.invitationIssued(invitationFor(carol, alice), "carol", alice);

        verifyNoInteractions(notifications);
    }

    @Test
    void the_warning_says_which_invitation_and_which_account_can_no_longer_be_named() {
        names.remove(alice);
        SpaceInvitation invitation = invitationFor(carol, alice);

        notifier.invitationIssued(invitation, "carol", alice);

        assertThat(logged.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly(
            "Space " + spaceId + ": invitation " + invitation.id() + " — account(s) [" + alice
                + "] can no longer be named, nobody is notified");
    }

    @Test
    void a_newcomer_is_announced_to_everyone_else() {
        membersAre(alice, bob, dave);

        notifier.memberJoined(spaceId, dave);

        verify(notifications).memberJoined(spaceId, "Chez nous", "dave", List.of(to(alice, "alice"), to(bob, "bob")));
    }

    @Test
    void nobody_else_in_the_space_means_nobody_is_told() {
        membersAre(dave);

        notifier.memberJoined(spaceId, dave);

        verifyNoInteractions(notifications);
    }

    @Test
    void a_departure_is_told_to_those_who_remain() {
        membersAre(alice, bob);

        notifier.memberLeft(spaceId, dave);

        verify(notifications).memberLeft(spaceId, "Chez nous", "dave", List.of(to(alice, "alice"), to(bob, "bob")));
    }

    @Test
    void a_removal_is_told_to_the_removed_and_to_the_others_but_never_to_its_author() {
        membersAre(alice, bob, carol);

        notifier.memberRemoved(spaceId, alice, dave);

        verify(notifications).removedFromSpace("Chez nous", "alice", to(dave, "dave"));
        verify(notifications).memberRemoved(spaceId, "Chez nous", "alice", "dave", List.of(to(bob, "bob"), to(carol, "carol")));
    }

    @Test
    void alone_with_the_author_only_the_removed_member_is_told() {
        membersAre(alice);

        notifier.memberRemoved(spaceId, alice, dave);

        verify(notifications).removedFromSpace("Chez nous", "alice", to(dave, "dave"));
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void a_deletion_is_told_to_every_member_but_the_owner_who_deleted() {
        membersAre(alice, bob, carol);

        notifier.spaceDeleted(spaceId, alice);

        verify(notifications).spaceDeleted("Chez nous", "alice", List.of(to(bob, "bob"), to(carol, "carol")));
    }

    @Test
    void a_role_change_is_told_to_the_member_concerned_only() {
        notifier.roleChanged(spaceId, alice, bob, SpaceRole.VIEWER, SpaceRole.MEMBER);

        verify(notifications).roleChanged(spaceId, "Chez nous", "alice", to(bob, "bob"), SpaceRole.VIEWER, SpaceRole.MEMBER);
    }

    @Test
    void a_transfer_tells_the_new_owner_and_the_others_but_not_the_former_owner() {
        membersAre(alice, bob, carol, dave);

        notifier.ownershipTransferred(spaceId, alice, bob);

        verify(notifications).ownershipReceived(spaceId, "Chez nous", "alice", to(bob, "bob"));
        verify(notifications).ownerChanged(spaceId, "Chez nous", "alice", "bob", List.of(to(carol, "carol"), to(dave, "dave")));
    }

    @Test
    void a_succession_names_the_former_owner_read_before_the_erasure() {
        membersAre(bob, carol);
        names.remove(alice);   // anonymised by now: only the caller still knows the name

        notifier.ownershipInherited(spaceId, alice, "alice", bob);

        verify(notifications).ownershipInherited(spaceId, "Chez nous", "alice", to(bob, "bob"));
        verify(notifications).ownerSucceeded(spaceId, "Chez nous", "alice", "bob", List.of(to(carol, "carol")));
    }

    @Test
    void a_member_who_left_nido_is_named_to_those_who_remain() {
        membersAre(alice, bob);
        names.remove(dave);

        notifier.memberLeftNido(spaceId, dave, "dave");

        verify(notifications).memberLeftNido(spaceId, "Chez nous", "dave", List.of(to(alice, "alice"), to(bob, "bob")));
    }

    @Test
    void an_erased_account_without_a_name_tells_nobody() {
        membersAre(alice, bob);
        names.remove(dave);

        notifier.memberLeftNido(spaceId, dave, null);
        notifier.ownershipInherited(spaceId, dave, null, alice);

        verifyNoInteractions(notifications);
        assertThat(logged.list).hasSize(2);
    }

    @Test
    void a_member_who_can_no_longer_be_named_is_left_out() {
        names.remove(carol);
        membersAre(alice, bob, carol, dave);

        notifier.memberJoined(spaceId, dave);

        verify(notifications).memberJoined(spaceId, "Chez nous", "dave", List.of(to(alice, "alice"), to(bob, "bob")));
    }

    @Test
    void an_event_whose_author_cannot_be_named_tells_nobody() {
        names.remove(alice);
        membersAre(alice, bob, carol);

        notifier.memberRemoved(spaceId, alice, dave);
        notifier.spaceDeleted(spaceId, alice);

        verifyNoInteractions(notifications);
    }

    @Test
    void a_space_that_is_not_there_tells_nobody() {
        UUID elsewhere = UUID.randomUUID();
        when(spaces.findById(elsewhere)).thenReturn(Optional.empty());

        notifier.memberJoined(elsewhere, dave);

        verifyNoInteractions(notifications);
    }

    private SpaceInvitation invitation(UUID invitee, InvitationStatus status, Instant expiresAt) {
        return new SpaceInvitation(UUID.randomUUID(), spaceId, invitee, SpaceRole.MEMBER, "NIDO-" + invitee,
            status, expiresAt, alice, null, now.minusSeconds(60));
    }

    @Test
    void a_revoked_invitation_is_told_to_its_invitee_by_the_name_of_who_revoked() {
        notifier.invitationRevoked(invitation(dave, InvitationStatus.PENDING, now.plusSeconds(60)), bob);

        verify(notifications).invitationCancelled("Chez nous", "bob", InvitationCancellation.REVOKED, List.of(to(dave, "dave")));
    }

    @Test
    void an_expired_invitation_is_revoked_silently() {
        notifier.invitationRevoked(invitation(dave, InvitationStatus.PENDING, now), bob);

        verifyNoInteractions(notifications);
    }

    @Test
    void a_deletion_tells_the_pending_invitees_their_invitation_is_cancelled() {
        membersAre(alice, bob);
        when(invitations.findBySpace(spaceId)).thenReturn(List.of(
            invitation(carol, InvitationStatus.PENDING, now.plusSeconds(60)),
            invitation(dave, InvitationStatus.PENDING, now.minusSeconds(1)),
            invitation(UUID.randomUUID(), InvitationStatus.ACCEPTED, now.plusSeconds(60))));

        notifier.spaceDeleted(spaceId, alice);

        verify(notifications).spaceDeleted("Chez nous", "alice", List.of(to(bob, "bob")));
        verify(notifications).invitationCancelled("Chez nous", "alice", InvitationCancellation.SPACE_DELETED,
            List.of(to(carol, "carol")));
    }

    @Test
    void a_space_alone_with_its_owner_still_tells_its_invitees() {
        membersAre(alice);
        when(invitations.findBySpace(spaceId)).thenReturn(List.of(invitation(carol, InvitationStatus.PENDING, now.plusSeconds(60))));

        notifier.spaceDeleted(spaceId, alice);

        verify(notifications).invitationCancelled("Chez nous", "alice", InvitationCancellation.SPACE_DELETED,
            List.of(to(carol, "carol")));
        verifyNoMoreInteractions(notifications);
    }

    @Test
    void a_space_lost_without_heir_tells_its_invitees_who_left_nido() {
        names.remove(alice);   // anonymised by now
        when(invitations.findBySpace(spaceId)).thenReturn(List.of(invitation(carol, InvitationStatus.PENDING, now.plusSeconds(60))));

        notifier.spaceDeletedWithoutHeir(spaceId, alice, "alice");

        verify(notifications).invitationCancelled("Chez nous", "alice", InvitationCancellation.OWNER_LEFT_NIDO,
            List.of(to(carol, "carol")));
    }
}
