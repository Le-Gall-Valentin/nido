package com.nido.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Demande de réinitialisation du mot de passe")
public record RequestPasswordResetRequest(
    @Schema(description = "Identifiant ou adresse mail du compte", example = "jane.doe", maxLength = 254)
    @NotBlank @Size(max = 254) String identifier
) {
    @Override
    public String toString() {
        return "RequestPasswordResetRequest[identifier=***]";
    }
}
