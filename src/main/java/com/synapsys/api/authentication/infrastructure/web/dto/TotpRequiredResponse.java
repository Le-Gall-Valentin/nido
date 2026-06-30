package com.synapsys.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indique que le login nécessite une vérification TOTP avant d'être finalisé")
public record TotpRequiredResponse(
    @Schema(description = "Toujours true — signale au client d'afficher l'écran de saisie du code TOTP", example = "true")
    boolean totpRequired
) {
    public TotpRequiredResponse() { this(true); }
}