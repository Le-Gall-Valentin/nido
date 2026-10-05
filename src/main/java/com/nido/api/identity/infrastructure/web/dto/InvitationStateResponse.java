package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.identity.domain.model.InvitationState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "L'invitation d'un compte qui n'a pas encore choisi son mot de passe")
public record InvitationStateResponse(
    @Schema(description = "`pending` : le lien marche encore ; `expired` : il faut en renvoyer un", allowableValues = {"pending", "expired"})
    String status,

    @Schema(description = "Fin de validité du lien (UTC)", example = "2026-10-12T10:00:00Z")
    Instant expiresAt
) {
    public static InvitationStateResponse of(InvitationState state) {
        return state == null ? null : new InvitationStateResponse(state.expired() ? "expired" : "pending", state.expiresAt());
    }
}
