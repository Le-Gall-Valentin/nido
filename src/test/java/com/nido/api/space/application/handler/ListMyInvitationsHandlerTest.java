package com.nido.api.space.application.handler;

import com.nido.api.space.application.service.MemberNames;
import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyInvitationsHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock SpaceRepository spaceRepository;
    @Mock MemberProfilePort memberProfilePort;

    private ListMyInvitationsHandler handler;

    private final UUID inviteeId = UUID.randomUUID();
    private final UUID spaceId = UUID.randomUUID();
    private final UUID otherSpaceId = UUID.randomUUID();
    private final UUID vanishedSpaceId = UUID.randomUUID();
    private final UUID aliceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListMyInvitationsHandler(spaceInvitationPort, spaceRepository, new MemberNames(memberProfilePort));
    }

    @Test
    void each_invitation_is_enriched_with_its_space_name_accent_and_glyph() {
        SpaceInvitation invitation = invitation(spaceId, aliceId);
        when(spaceInvitationPort.findPendingForInvitee(eq(inviteeId), any())).thenReturn(List.of(invitation));
        when(spaceRepository.findByIds(List.of(spaceId))).thenReturn(List.of(sharedSpace(spaceId)));

        List<ReceivedInvitationView> result = handler.listMine(inviteeId);

        assertThat(result).hasSize(1);
        ReceivedInvitationView view = result.get(0);
        assertThat(view.invitationId()).isEqualTo(invitation.id());
        assertThat(view.spaceId()).isEqualTo(spaceId);
        assertThat(view.spaceName()).isEqualTo("Chez Valentin");
        assertThat(view.spaceAccent()).isEqualTo("#4a7fa0");
        assertThat(view.spaceGlyph()).isEqualTo("🏠");
        assertThat(view.role()).isEqualTo(SpaceRole.MEMBER);
        assertThat(view.expiresAt()).isEqualTo(invitation.expiresAt());
    }

    @Test
    void each_invitation_names_who_sent_it_resolved_in_one_call_for_the_whole_list() {
        when(spaceInvitationPort.findPendingForInvitee(eq(inviteeId), any()))
            .thenReturn(List.of(invitation(spaceId, aliceId), invitation(otherSpaceId, aliceId)));
        when(spaceRepository.findByIds(List.of(spaceId, otherSpaceId)))
            .thenReturn(List.of(sharedSpace(spaceId), sharedSpace(otherSpaceId)));
        when(memberProfilePort.findByIds(List.of(aliceId)))
            .thenReturn(List.of(new MemberProfile(aliceId, "alice", "alice@example.com")));

        List<ReceivedInvitationView> result = handler.listMine(inviteeId);

        assertThat(result).extracting(ReceivedInvitationView::invitedByUsername).containsExactly("alice", "alice");
        verify(memberProfilePort, times(1)).findByIds(anyCollection());
    }

    @Test
    void an_invitation_from_an_anonymized_or_unknown_inviter_lists_without_a_name() {
        UUID anonymizedId = UUID.randomUUID();
        when(spaceInvitationPort.findPendingForInvitee(eq(inviteeId), any()))
            .thenReturn(List.of(invitation(spaceId, anonymizedId), invitation(otherSpaceId, null)));
        when(spaceRepository.findByIds(List.of(spaceId, otherSpaceId)))
            .thenReturn(List.of(sharedSpace(spaceId), sharedSpace(otherSpaceId)));
        // GDPR anonymization keeps the account row but blanks its username.
        when(memberProfilePort.findByIds(List.of(anonymizedId)))
            .thenReturn(List.of(new MemberProfile(anonymizedId, null, null)));

        List<ReceivedInvitationView> result = handler.listMine(inviteeId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ReceivedInvitationView::invitedByUsername).containsOnlyNulls();
    }

    @Test
    void an_invitation_whose_space_has_vanished_is_skipped_rather_than_failing_the_list() {
        SpaceInvitation orphan = invitation(vanishedSpaceId, aliceId);
        when(spaceInvitationPort.findPendingForInvitee(eq(inviteeId), any())).thenReturn(List.of(orphan));
        when(spaceRepository.findByIds(List.of(vanishedSpaceId))).thenReturn(List.of());

        List<ReceivedInvitationView> result = handler.listMine(inviteeId);

        assertThat(result).isEmpty();
    }

    @Test
    void asks_for_no_profile_when_there_is_no_invitation() {
        when(spaceInvitationPort.findPendingForInvitee(eq(inviteeId), any())).thenReturn(List.of());

        assertThat(handler.listMine(inviteeId)).isEmpty();
        verify(memberProfilePort, never()).findByIds(anyCollection());
    }

    private SpaceInvitation invitation(UUID onSpaceId, UUID createdBy) {
        return new SpaceInvitation(UUID.randomUUID(), onSpaceId, inviteeId, SpaceRole.MEMBER, "NIDO-ABC123",
            InvitationStatus.PENDING, Instant.now().plusSeconds(3600), createdBy, null, Instant.now());
    }

    private Space sharedSpace(UUID id) {
        return new Space(id, SpaceType.SHARED, "Chez Valentin", null, "#4a7fa0", "🏠", null, ZoneId.of("Europe/Paris"), Instant.now());
    }
}
