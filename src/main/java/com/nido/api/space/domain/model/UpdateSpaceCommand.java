package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

public record UpdateSpaceCommand(
    UUID spaceId,
    String name,
    String description,
    String accent,
    String glyph
) {
    public UpdateSpaceCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        name = SpaceText.requireName(name);
        description = SpaceText.normalizeDescription(description);
        SpaceAppearance.ensureValid(accent, glyph);
    }
}
