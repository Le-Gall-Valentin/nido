package com.nido.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Indique que le login nécessite une vérification TOTP avant d'être finalisé")
public record TotpRequiredResponse(
    @Schema(description = "Toujours true — signale au client d'afficher l'écran de saisie du code TOTP", example = "true")
    boolean totpRequired,

    @Schema(description = "Nom d'utilisateur du compte, pour saluer la personne sur l'écran du code même si elle a tapé son adresse. Envoyé seulement après le bon mot de passe.", example = "john.doe")
    String username
) {
    public TotpRequiredResponse(String username) { this(true, username); }
}
