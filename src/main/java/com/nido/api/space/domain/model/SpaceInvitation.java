package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpaceInvitation(
    UUID id,
    UUID spaceId,
    UUID inviteeId,
    SpaceRole role,
    String code,
    InvitationStatus status,
    Instant expiresAt,
    UUID createdBy,
    Instant acceptedAt,
    Instant createdAt
) {
    public SpaceInvitation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(inviteeId, "inviteeId");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void ensurePending() {
        if (status != InvitationStatus.PENDING) {
            throw new SpaceException.InvitationNotPending();
        }
    }

    public void ensureNotExpired(Instant now) {
        if (isExpired(now)) {
            throw new SpaceException.InvitationExpired();
        }
    }

    /**
     * Vérifie que l'appelant est bien le compte invité.
     *
     * <p>C'est ce contrôle, et lui seul, qui rend légitime le stockage du code en clair :
     * détenir le code ne suffit pas, il faut être authentifié comme la personne invitée.
     * Le relâcher — pour offrir un lien d'invitation ouvert par exemple — transformerait
     * le code en jeton porteur et rendrait le stockage en clair fautif.
     */
    public void ensureAddressedTo(UUID callerId) {
        if (!inviteeId.equals(callerId)) {
            throw new SpaceException.InvitationForAnotherAccount();
        }
    }
}
