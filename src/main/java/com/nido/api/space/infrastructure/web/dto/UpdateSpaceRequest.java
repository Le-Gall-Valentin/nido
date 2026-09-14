package com.nido.api.space.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Modification partielle d'un groupe : tout champ absent reste inchangé")
public record UpdateSpaceRequest(
    @Schema(description = "Nom du groupe. Absent : inchangé. Obligatoire, donc non effaçable.",
        example = "Chez Valentin")
    @Size(max = 80) String name,

    @Schema(description = "Description. Absent : inchangé. Chaîne vide : effacement.",
        example = "Notre appartement à trois")
    @Size(max = 280) String description,

    @Schema(description = "Couleur d'accent, parmi la palette autorisée. Absent : inchangé.",
        example = "#c17a5c")
    @Size(max = 7) String accent,

    @Schema(description = "Glyphe, parmi la liste autorisée. Absent : inchangé.", example = "🏡")
    @Size(max = 8) String glyph,

    @Schema(description = """
        Fuseau horaire du contexte, identifiant IANA. Absent : inchangé.

        C'est lui qui décide de ce qu'« aujourd'hui » veut dire pour ce contexte — ce qui est dû,
        ce qui est en retard, quel mois s'ouvre. Un contexte partagé s'accorde donc sur un seul
        fuseau pour tous ses membres ; un contexte personnel n'ayant qu'un membre, c'est le sien.

        Les décalages fixes (`+02:00`) sont refusés : ils ne suivent pas l'heure d'été.
        """,
        example = "Europe/Paris")
    @Size(max = 64) String timezone
) {}
