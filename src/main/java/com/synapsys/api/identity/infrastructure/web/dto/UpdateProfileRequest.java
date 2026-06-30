package com.synapsys.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Données de mise à jour du profil de l'utilisateur connecté")
public record UpdateProfileRequest(
    @Schema(description = "Nouveau nom d'utilisateur (3–50 caractères)", example = "john.updated", minLength = 3, maxLength = 50)
    @NotBlank @Size(min = 3, max = 50) String username,

    @Schema(description = "Nouvelle adresse email (max 254 caractères)", example = "john.updated@example.com", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String email
) {}