package com.nido.api.dashboard.domain.model;

import com.nido.api.space.domain.model.SpaceRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PendingInvitationsTest {

    @Test
    void theInvitationThatExpiresSoonestComesFirst() {
        AttentionItem.Invitation later = invitation("Coloc Lyon", Instant.parse("2026-10-05T09:00:00Z"));
        AttentionItem.Invitation sooner = invitation("Club de lecture", Instant.parse("2026-09-28T09:00:00Z"));

        SourceResult result = PendingInvitations.of(List.of(later, sooner));

        assertThat(result.card()).isNull();
        assertThat(result.attention()).containsExactly(sooner, later);
    }

    @Test
    void noInvitationMeansNothingToDo() {
        SourceResult result = PendingInvitations.of(List.of());

        assertThat(result.card()).isNull();
        assertThat(result.attention()).isEmpty();
    }

    private static AttentionItem.Invitation invitation(String spaceName, Instant expiresAt) {
        return new AttentionItem.Invitation(UUID.randomUUID(), spaceName, "🏠", "#c17a5c", SpaceRole.MEMBER, "camille", expiresAt);
    }
}
