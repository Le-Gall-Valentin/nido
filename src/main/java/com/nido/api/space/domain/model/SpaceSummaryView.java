package com.nido.api.space.domain.model;

import java.time.ZoneId;
import java.util.UUID;

public record SpaceSummaryView(
    UUID id,
    SpaceType type,
    String name,
    String accent,
    String glyph,
    /** So a screen showing this space knows what "today" means in it, without a second read. */
    ZoneId timezone,
    SpaceRole myRole,
    long memberCount
) {}
