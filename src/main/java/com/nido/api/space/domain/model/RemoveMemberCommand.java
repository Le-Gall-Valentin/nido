package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

public record RemoveMemberCommand(UUID spaceId, UUID targetUserId) {
    public RemoveMemberCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(targetUserId, "targetUserId");
    }
}
