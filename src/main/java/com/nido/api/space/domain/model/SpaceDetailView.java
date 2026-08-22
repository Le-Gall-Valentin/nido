package com.nido.api.space.domain.model;

import java.util.UUID;

public record SpaceDetailView(
    UUID id,
    SpaceType type,
    String name,
    String description,
    String accent,
    String glyph,
    SpaceRole myRole,
    long memberCount
) {}
