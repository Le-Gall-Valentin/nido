package com.synapsys.api.authentication.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Credentials de connexion")
public record LoginRequest(
    @Schema(description = "Nom d'utilisateur", example = "john.doe", maxLength = 50)
    @NotBlank @Size(max = 50) String username,

    @Schema(description = "Mot de passe", example = "S3cr3t!Pass", maxLength = 72)
    @NotBlank @Size(max = 72) String password
) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + "]";
    }
}