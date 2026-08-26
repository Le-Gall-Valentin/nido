package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Métadonnées visibles par un administrateur de plateforme. Jamais de contenu métier. */
public record SpaceAdminView(
    UUID id,
    SpaceType type,
    String name,
    long memberCount,
    UUID createdBy,
    Instant createdAt
) {}
