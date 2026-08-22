package com.nido.api.space.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CreateSharedSpaceCommand(
    String name,
    String description,
    String accent,
    String glyph,
    UUID creatorUserId
) {
    public static final int NAME_MAX_LENGTH = 80;
    public static final int DESCRIPTION_MAX_LENGTH = 280;

    public CreateSharedSpaceCommand {
        Objects.requireNonNull(creatorUserId, "creatorUserId");
        name = SpaceText.requireName(name);
        description = SpaceText.normalizeDescription(description);
        SpaceAppearance.ensureValid(accent, glyph);
    }
}
