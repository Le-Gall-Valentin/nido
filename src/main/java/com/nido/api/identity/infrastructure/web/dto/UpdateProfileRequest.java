package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.identity.infrastructure.web.validation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Données de mise à jour du profil de l'utilisateur connecté")
public record UpdateProfileRequest(
    @Schema(description = "Nouveau nom d'utilisateur (3–50 caractères, sans @)", example = "john.updated", minLength = 3, maxLength = 50)
    @ValidUsername String username,

    @Schema(description = "Nouvelle adresse email (max 254 caractères)", example = "john.updated@example.com", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String email,

    @Schema(description = "Mot de passe actuel — obligatoire si l'adresse change (elle sert à récupérer le compte)", maxLength = 72)
    @Size(max = 72) String currentPassword,

    @Schema(description = "Le code reçu à la nouvelle adresse, quand la double authentification par mail est active ; absent au premier envoi", maxLength = 6, nullable = true)
    @Pattern(regexp = "\\d{6}", message = "must be exactly 6 digits") String emailCode
) {
    @Override
    public String toString() {
        return "UpdateProfileRequest[username=" + username + ", email=***, currentPassword=***, emailCode=***]";
    }
}