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
