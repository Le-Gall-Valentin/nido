package com.nido.api.dashboard.infrastructure.source;

import com.nido.api.dashboard.domain.model.AttentionItem;
import com.nido.api.dashboard.domain.model.CardKind;
import com.nido.api.dashboard.domain.model.DashboardContext;
import com.nido.api.dashboard.domain.model.SourceResult;
import com.nido.api.space.application.port.in.ListMyInvitationsUseCase;
import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.SpaceMembership;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InvitationsDashboardSourceTest {

    private final SpaceMembership caller = new SpaceMembership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), SpaceRole.MEMBER, Instant.now());
    private final ListMyInvitationsUseCase listMine = mock(ListMyInvitationsUseCase.class);

    @Test
    void anInvitationSentToTheCallersAddressBecomesAnItem() {
        // The order is PendingInvitations' (see PendingInvitationsTest); this is the translation.
        ReceivedInvitationView invitation = view("Club de lecture", "paul", Instant.parse("2026-09-28T09:00:00Z"));
        when(listMine.listMine("alice@test.com")).thenReturn(List.of(invitation));

        SourceResult result = read("alice@test.com");

        assertThat(result.card()).isNull();
        assertThat(result.attention()).containsExactly(new AttentionItem.Invitation(invitation.invitationId(),
            "Club de lecture", "🏠", "#c17a5c", SpaceRole.MEMBER, "paul", Instant.parse("2026-09-28T09:00:00Z")));
    }

    @Test
    void anAnonymizedInviterStaysUnnamed() {
        when(listMine.listMine("alice@test.com")).thenReturn(List.of(
            view("Coloc Lyon", null, Instant.parse("2026-10-05T09:00:00Z"))));

        assertThat(read("alice@test.com").attention())
            .extracting(item -> ((AttentionItem.Invitation) item).invitedByUsername())
            .containsOnlyNulls();
    }

    @Test
    void itIsTheInvitationsSource() {
        assertThat(new InvitationsDashboardSource(listMine).kind()).isEqualTo(CardKind.INVITATIONS);
    }

    private SourceResult read(String email) {
        return new InvitationsDashboardSource(listMine)
            .read(new DashboardContext(caller, email, LocalDate.of(2026, 9, 26), SpaceType.SHARED));
    }

    private static ReceivedInvitationView view(String spaceName, String invitedBy, Instant expiresAt) {
        return new ReceivedInvitationView(UUID.randomUUID(), UUID.randomUUID(), spaceName, "#c17a5c", "🏠",
            SpaceRole.MEMBER, expiresAt, invitedBy);
    }
}
