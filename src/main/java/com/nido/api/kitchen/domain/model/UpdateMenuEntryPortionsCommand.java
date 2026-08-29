package com.nido.api.kitchen.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UpdateMenuEntryPortionsCommand(UUID entryId, UUID spaceId, int portions) {
    public UpdateMenuEntryPortionsCommand {
        Objects.requireNonNull(entryId, "entryId");
        Objects.requireNonNull(spaceId, "spaceId");
    }
}
