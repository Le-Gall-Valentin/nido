package com.nido.api.space.domain.model;

import java.util.UUID;

public record SpaceSummaryView(
    UUID id,
    SpaceType type,
    String name,
    String accent,
    String glyph,
    SpaceRole myRole,
    long memberCount
) {}
