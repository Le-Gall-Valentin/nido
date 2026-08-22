package com.nido.api.space.application.handler;

import com.nido.api.space.domain.model.InvitationStatus;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.Space;
import com.nido.api.space.domain.model.SpaceInvitation;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import com.nido.api.space.domain.port.out.SpaceInvitationPort;
import com.nido.api.space.domain.port.out.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListMyInvitationsHandlerTest {

    @Mock SpaceInvitationPort spaceInvitationPort;
    @Mock SpaceRepository spaceRepository;

    private ListMyInvitationsHandler handler;

    private final String email = "carol@example.com";
    private final UUID spaceId = UUID.randomUUID();
    private final UUID vanishedSpaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new ListMyInvitationsHandler(spaceInvitationPort, spaceRepository);
    }

    @Test
    void each_invitation_is_enriched_with_its_space_name_accent_and_glyph() {
        SpaceInvitation invitation = invitation(spaceId);
        when(spaceInvitationPort.findPendingForEmail(eq(email), any())).thenReturn(List.of(invitation));
        when(spaceRepository.findByIds(List.of(spaceId))).thenReturn(List.of(sharedSpace(spaceId)));

        List<ReceivedInvitationView> result = handler.listMine(email);

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
    void an_invitation_whose_space_has_vanished_is_skipped_rather_than_failing_the_list() {
        SpaceInvitation orphan = invitation(vanishedSpaceId);
        when(spaceInvitationPort.findPendingForEmail(eq(email), any())).thenReturn(List.of(orphan));
        when(spaceRepository.findByIds(List.of(vanishedSpaceId))).thenReturn(List.of());

        List<ReceivedInvitationView> result = handler.listMine(email);

        assertThat(result).isEmpty();
    }

    private SpaceInvitation invitation(UUID onSpaceId) {
        return new SpaceInvitation(UUID.randomUUID(), onSpaceId, email, SpaceRole.MEMBER, "NIDO-ABC123",
            InvitationStatus.PENDING, Instant.now().plusSeconds(3600), UUID.randomUUID(), null, Instant.now());
    }

    private Space sharedSpace(UUID id) {
        return new Space(id, SpaceType.SHARED, "Chez Valentin", null, "#4a7fa0", "🏠", null, Instant.now());
    }
}
