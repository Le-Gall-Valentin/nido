package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceDetailView;
import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceType;

import java.util.UUID;

public record SpaceDetailResponse(
    UUID id,
    SpaceType type,
    String name,
    String description,
    String accent,
    String glyph,
    SpaceRole myRole,
    long memberCount
) {
    public static SpaceDetailResponse from(SpaceDetailView view) {
        return new SpaceDetailResponse(view.id(), view.type(), view.name(), view.description(),
            view.accent(), view.glyph(), view.myRole(), view.memberCount());
    }
}
