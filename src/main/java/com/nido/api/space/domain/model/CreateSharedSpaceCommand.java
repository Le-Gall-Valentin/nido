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
    // Source unique : les bornes vivent dans SpaceText, qui les fait respecter.
    public static final int NAME_MAX_LENGTH = SpaceText.NAME_MAX_LENGTH;
    public static final int DESCRIPTION_MAX_LENGTH = SpaceText.DESCRIPTION_MAX_LENGTH;

    public CreateSharedSpaceCommand {
        Objects.requireNonNull(creatorUserId, "creatorUserId");
        name = SpaceText.requireName(name);
        description = SpaceText.descriptionOnCreate(description);
        SpaceAppearance.ensureValid(accent, glyph);
    }
}
