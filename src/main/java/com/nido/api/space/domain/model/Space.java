package com.nido.api.space.domain.model;

import java.time.Instant;
import java.time.ZoneId;
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
    /**
     * The clock this space keeps. A household agrees on one calendar — the rent is due on the
     * household's date, not on the date of whichever member happens to be travelling — so "today"
     * is a property of the space and not of the viewer. A personal space has exactly one member,
     * so its zone is that member's own; the two ideas are the same mechanism.
     */
    ZoneId timezone,
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
