package com.nido.api.mfa.infrastructure.web.dto;

import com.nido.api.mfa.domain.model.MethodState;
import com.nido.api.shared.model.TwoFactorMethod;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Une méthode de double authentification du compte connecté")
public record MethodStateResponse(
    @Schema(description = "La méthode", example = "MAIL") TwoFactorMethod method,
    @Schema(description = "Activée sur le compte", example = "true") boolean enabled,
    @Schema(description = "Utilisable maintenant — `MAIL` ne l'est pas tant que l'envoi de mails est coupé ; activée et inutilisable = en pause", example = "true") boolean usable
) {
    public static MethodStateResponse of(MethodState state) {
        return new MethodStateResponse(state.method(), state.enabled(), state.usable());
    }
}
