package com.nido.api.space.infrastructure.web.dto;

import com.nido.api.space.domain.model.ReceivedInvitationView;
import com.nido.api.space.domain.model.SpaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Une invitation reçue, adressée à l'utilisateur connecté")
public record ReceivedInvitationResponse(
    @Schema(description = "Identifiant de l'invitation") UUID invitationId,
    @Schema(description = "Identifiant du contexte") UUID spaceId,
    @Schema(description = "Nom du contexte") String spaceName,
    @Schema(description = "Couleur d'accent du contexte") String spaceAccent,
    @Schema(description = "Glyphe du contexte") String spaceGlyph,
    @Schema(description = "Rôle proposé par l'invitation") SpaceRole role,
    @Schema(description = "Date d'expiration") Instant expiresAt
) {
    public static ReceivedInvitationResponse from(ReceivedInvitationView view) {
        return new ReceivedInvitationResponse(view.invitationId(), view.spaceId(), view.spaceName(),
            view.spaceAccent(), view.spaceGlyph(), view.role(), view.expiresAt());
    }
}
