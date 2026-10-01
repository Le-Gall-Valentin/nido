package com.nido.api.space.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpaceInvitationTest {

    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");
    private static final UUID INVITEE = UUID.randomUUID();

    @Test
    void a_pending_invitation_within_its_validity_passes_every_guard() {
        SpaceInvitation invitation = invitation(InvitationStatus.PENDING, NOW.plus(Duration.ofDays(7)));

        assertThatCode(invitation::ensurePending).doesNotThrowAnyException();
        assertThatCode(() -> invitation.ensureNotExpired(NOW)).doesNotThrowAnyException();
        assertThat(invitation.isExpired(NOW)).isFalse();
    }

    @Test
    void an_accepted_or_revoked_invitation_is_no_longer_pending() {
        assertThatThrownBy(() -> invitation(InvitationStatus.ACCEPTED, NOW.plus(Duration.ofDays(7))).ensurePending())
            .isInstanceOf(SpaceException.InvitationNotPending.class);
        assertThatThrownBy(() -> invitation(InvitationStatus.REVOKED, NOW.plus(Duration.ofDays(7))).ensurePending())
            .isInstanceOf(SpaceException.InvitationNotPending.class);
    }

    @Test
    void an_invitation_past_its_expiry_is_refused() {
        SpaceInvitation invitation = invitation(InvitationStatus.PENDING, NOW.minusSeconds(1));

        assertThat(invitation.isExpired(NOW)).isTrue();
        assertThatThrownBy(() -> invitation.ensureNotExpired(NOW))
            .isInstanceOf(SpaceException.InvitationExpired.class);
    }

    @Test
    void the_account_binding_is_what_makes_a_clear_text_code_safe() {
        // Garde structurante : sans elle, le code deviendrait un jeton porteur.
        SpaceInvitation invitation = invitation(InvitationStatus.PENDING, NOW.plus(Duration.ofDays(7)));

        assertThatCode(() -> invitation.ensureAddressedTo(INVITEE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> invitation.ensureAddressedTo(UUID.randomUUID()))
            .isInstanceOf(SpaceException.InvitationForAnotherAccount.class);
        assertThatThrownBy(() -> invitation.ensureAddressedTo(null))
            .isInstanceOf(SpaceException.InvitationForAnotherAccount.class);
    }

    private static SpaceInvitation invitation(InvitationStatus status, Instant expiresAt) {
        return new SpaceInvitation(UUID.randomUUID(), UUID.randomUUID(), INVITEE,
            SpaceRole.MEMBER, "NIDO-4F9C2A", status, expiresAt, UUID.randomUUID(), null, NOW);
    }
}
