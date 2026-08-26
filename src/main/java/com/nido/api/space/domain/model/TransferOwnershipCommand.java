package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TransferOwnershipCommand(UUID spaceId, UUID newOwnerUserId) {
    public TransferOwnershipCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        Objects.requireNonNull(newOwnerUserId, "newOwnerUserId");
    }
}
