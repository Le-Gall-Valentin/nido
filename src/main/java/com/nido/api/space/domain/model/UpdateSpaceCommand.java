package com.nido.api.space.domain.model;

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
    String glyph
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
}
