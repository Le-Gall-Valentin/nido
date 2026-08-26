package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SpaceMembership(
    UUID id,
    UUID spaceId,
    UUID userId,
    SpaceRole role,
    Instant joinedAt
) {
    public SpaceMembership {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(role, "role");
    }

    /**
     * Vérifie que cette adhésion autorise bien le contexte visé par la commande.
     * Une divergence est traitée comme une absence d'adhésion : le contexte demandé
     * n'est pas celui dont l'appelant a prouvé être membre.
     */
    public void ensureSameSpace(UUID targetSpaceId) {
        if (!spaceId.equals(targetSpaceId)) {
            throw new SpaceException.NotAMember();
        }
    }

    public boolean isOwner() {
        return role == SpaceRole.OWNER;
    }

    public void ensureCanWrite() {
        if (!role.canWrite()) {
            throw new SpaceException.InsufficientRole();
        }
    }

    public void ensureCanManageSpace() {
        if (!role.canManageSpace()) {
            throw new SpaceException.InsufficientRole();
        }
    }

    public void ensureOwner() {
        if (!isOwner()) {
            throw new SpaceException.OwnerRequired();
        }
    }
}
