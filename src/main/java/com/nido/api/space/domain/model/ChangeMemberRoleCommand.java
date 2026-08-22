package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

public record ChangeMemberRoleCommand(UUID spaceId, UUID targetUserId, SpaceRole newRole) {
    public ChangeMemberRoleCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(targetUserId, "targetUserId");
        Objects.requireNonNull(newRole, "newRole");
    }
}
