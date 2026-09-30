package com.nido.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import com.nido.api.shared.validation.StrongPassword;

@Schema(description = "Données pour le changement de mot de passe de l'utilisateur connecté")
public record ChangePasswordRequest(
    @Schema(description = "Mot de passe actuel", example = "OldP4ss!")
    @NotBlank String currentPassword,

    @Schema(description = "Nouveau mot de passe (8 caractères minimum, 72 octets maximum — une lettre accentuée en compte deux —, au moins une majuscule, un chiffre et un caractère spécial). Doit être différent du mot de passe actuel.", example = "NewS3cur3!", minLength = 8, maxLength = 72)
    @StrongPassword
    String newPassword
) {
    @AssertTrue(message = "new password must differ from current password")
    public boolean isNewPasswordDifferent() {
        return currentPassword == null || newPassword == null || !newPassword.equals(currentPassword);
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest[]";
    }
}