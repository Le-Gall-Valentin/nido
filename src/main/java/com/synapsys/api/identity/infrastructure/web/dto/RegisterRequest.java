package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour la création d'un nouvel utilisateur")
public record RegisterRequest(
    @Schema(description = "Nom d'utilisateur (3–50 caractères)", example = "jane.doe", minLength = 3, maxLength = 50)
    @NotBlank @Size(min = 3, max = 50) String username,

    @Schema(description = "Adresse email valide (max 254 caractères)", example = "jane.doe@example.com", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String email,

    @Schema(description = "Mot de passe (8–72 caractères, au moins une majuscule, un chiffre et un caractère spécial)", example = "S3cr3t!Pass", minLength = 8, maxLength = 72)
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "must contain at least one uppercase letter, one digit, and one special character"
    )
    String password,

    @Schema(description = "Rôle attribué au nouvel utilisateur. Un ADMIN ne peut pas créer un SUPER_ADMIN.")
    @NotNull Role role
) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=" + email + ", role=" + role + "]";
    }
}