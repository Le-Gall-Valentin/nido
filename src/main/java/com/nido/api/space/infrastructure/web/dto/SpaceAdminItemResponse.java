package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceAdminView;
import com.nido.api.space.domain.model.SpaceType;

import java.time.Instant;
import java.util.UUID;

/** Métadonnées seulement : pas d'accent, pas de description, aucun contenu métier. */
public record SpaceAdminItemResponse(
    UUID id,
    SpaceType type,
    String name,
    long memberCount,
    UUID createdBy,
    Instant createdAt
) {
    public static SpaceAdminItemResponse from(SpaceAdminView view) {
        return new SpaceAdminItemResponse(view.id(), view.type(), view.name(),
            view.memberCount(), view.createdBy(), view.createdAt());
    }
}
