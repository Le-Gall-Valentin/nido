package com.nido.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Jeton d'un lien de réinitialisation")
public record PasswordResetTokenRequest(
    @Schema(description = "Le jeton lu après le # du lien", maxLength = 128)
    @NotBlank @Size(max = 128) String token
) {
    @Override
    public String toString() {
        return "PasswordResetTokenRequest[token=***]";
    }
}
