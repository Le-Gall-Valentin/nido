package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.Role;
import com.nido.api.identity.infrastructure.web.validation.ValidUsername;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour la création d'un nouvel utilisateur — il choisira son mot de passe par le lien d'invitation")
public record RegisterRequest(
    @Schema(description = "Nom d'utilisateur (3–50 caractères, sans @)", example = "jane.doe", minLength = 3, maxLength = 50)
    @ValidUsername String username,

    @Schema(description = "Adresse email valide (max 254 caractères)", example = "jane.doe@example.com", maxLength = 254)
    @NotBlank @Email @Size(max = 254) String email,

    @Schema(description = "Rôle attribué au nouvel utilisateur. Un ADMIN ne peut pas créer un SUPER_ADMIN.")
    @NotNull Role role
) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", email=***, role=" + role + "]";
    }
}