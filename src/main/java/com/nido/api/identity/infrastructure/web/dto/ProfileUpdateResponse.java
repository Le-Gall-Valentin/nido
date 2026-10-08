package com.nido.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Rien n'est enregistré : un code est parti à la nouvelle adresse")
public record ProfileUpdateResponse(
    @Schema(description = "Toujours true", example = "true") boolean emailCodeRequired,
    @Schema(description = "La nouvelle adresse, où le code est parti", example = "jane.doe@example.fr") String sentTo,
    @Schema(description = "Secondes avant de pouvoir demander un nouveau code", example = "60") long resendAfterSeconds
) {}
