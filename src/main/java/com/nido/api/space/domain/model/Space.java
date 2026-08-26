package com.nido.api.space.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Space(
    UUID id,
    SpaceType type,
    String name,
    String description,
    String accent,
    String glyph,
    UUID personalOwnerId,
    Instant createdAt
) {
    public Space {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(accent, "accent");
        Objects.requireNonNull(glyph, "glyph");
    }

    public boolean isPersonal() {
        return type == SpaceType.PERSONAL;
    }

    /** L'espace perso ne se renomme pas, ne se supprime pas, ne se partage pas. */
    public void ensureShared() {
        if (isPersonal()) {
            throw new SpaceException.PersonalSpaceImmutable();
        }
    }
}
