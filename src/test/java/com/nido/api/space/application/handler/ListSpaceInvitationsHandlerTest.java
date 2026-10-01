package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.MemberProfile;
import com.nido.api.space.domain.model.SpaceException;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.port.out.MemberProfilePort;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListSpaceInvitationsHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock MemberProfilePort memberProfilePort;

    private ListSpaceInvitationsHandler handler;

    private final UUID spaceId = UUID.randomUUID();
    private final UUID carolId = UUID.randomUUID();
    private final UUID daveId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListSpaceInvitationsHandler(spaceInvitationPort, memberProfilePort);
    }

    @Test
    void each_invitation_names_its_invitee_by_username_resolved_in_one_call() {
        when(spaceInvitationPort.findBySpace(spaceId)).thenReturn(List.of(invitationFor(carolId), invitationFor(daveId)));
        when(memberProfilePort.findByIds(List.of(carolId, daveId))).thenReturn(List.of(
            new MemberProfile(carolId, "carol", "carol@test.com"), new MemberProfile(daveId, "dave", "dave@test.com")));

        List<SpaceInvitationView> views = handler.list(withRole(SpaceRole.ADMIN));

        assertThat(views).extracting(SpaceInvitationView::username).containsExactly("carol", "dave");
        verify(memberProfilePort, times(1)).findByIds(any());
    }

    @Test
    void an_invitee_who_can_no_longer_be_resolved_lists_without_a_name() {
        when(spaceInvitationPort.findBySpace(spaceId)).thenReturn(List.of(invitationFor(carolId)));
        when(memberProfilePort.findByIds(List.of(carolId))).thenReturn(List.of());

        assertThat(handler.list(withRole(SpaceRole.ADMIN))).extracting(SpaceInvitationView::username).containsOnlyNulls();
    }

    @Test
    void a_plain_member_cannot_list_them() {
        assertThatThrownBy(() -> handler.list(withRole(SpaceRole.MEMBER)))
            .isInstanceOf(SpaceException.InsufficientRole.class);
        verifyNoInteractions(spaceInvitationPort, memberProfilePort);
    }

    private SpaceMembership withRole(SpaceRole role) {
        return new SpaceMembership(UUID.randomUUID(), spaceId, UUID.randomUUID(), role, Instant.now());
    }

    private SpaceInvitation invitationFor(UUID inviteeId) {
        return new SpaceInvitation(UUID.randomUUID(), spaceId, inviteeId, SpaceRole.MEMBER, "NIDO-ABC123",
            InvitationStatus.PENDING, Instant.now().plusSeconds(3600), UUID.randomUUID(), null, Instant.now());
    }
}
