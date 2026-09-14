package com.nido.api.space.domain.model;

import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Modification partielle : un champ absent (null) laisse la valeur en place. Le nom,
 * l'accent et le glyphe sont obligatoires, donc remplaçables mais pas effaçables ;
 * la description s'efface en envoyant une chaîne vide ou blanche.
 */
public record UpdateSpaceCommand(
    UUID spaceId,
    String name,
    String description,
    String accent,
    String glyph,
    /** Absent leaves the space's calendar alone — see the class comment on partial updates. */
    ZoneId timezone
) {
    public UpdateSpaceCommand {
        Objects.requireNonNull(spaceId, "spaceId");
        name = SpaceText.nameIfPresent(name);
        description = SpaceText.descriptionOnUpdate(description);
        if (accent != null) {
            SpaceAppearance.ensureValidAccent(accent);
        }
        if (glyph != null) {
            SpaceAppearance.ensureValidGlyph(glyph);
        }
    }

    /**
     * Whether this update touches what the space <em>is</em>, as opposed to how it behaves.
     *
     * <p>The personal space has no identity to change — one member, a fixed name, a fixed
     * appearance — and that is deliberate. It still keeps a calendar like any other space, and its
     * owner is the only person that calendar concerns, so refusing every update was the coarse
     * version of the rule rather than the rule.
     */
    public boolean changesIdentity() {
        return name != null || description != null || accent != null || glyph != null;
    }
}
