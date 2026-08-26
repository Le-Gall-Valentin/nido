package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.SpaceRole;
import com.nido.api.space.domain.model.SpaceSummaryView;
import com.nido.api.space.domain.model.SpaceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Un contexte auquel l'utilisateur appartient")
public record SpaceSummaryResponse(
    @Schema(description = "Identifiant du contexte") UUID id,
    @Schema(description = "PERSONAL pour l'espace perso, SHARED pour un groupe") SpaceType type,
    @Schema(description = "Nom du contexte") String name,
    @Schema(description = "Couleur d'accent") String accent,
    @Schema(description = "Glyphe") String glyph,
    @Schema(description = "Rôle de l'utilisateur dans ce contexte") SpaceRole myRole,
    @Schema(description = "Nombre de membres") long memberCount
) {
    public static SpaceSummaryResponse from(SpaceSummaryView view) {
        return new SpaceSummaryResponse(view.id(), view.type(), view.name(),
            view.accent(), view.glyph(), view.myRole(), view.memberCount());
    }
}
