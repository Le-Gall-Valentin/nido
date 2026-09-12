package com.nido.api.space.domain.model;

import java.time.ZoneId;
import java.util.UUID;

public record SpaceDetailView(
    UUID id,
    SpaceType type,
    String name,
    String description,
    String accent,
    String glyph,
    /** The calendar this space keeps — what "today" means for everything inside it. */
    ZoneId timezone,
    SpaceRole myRole,
    long memberCount
) {}
