package com.synapsys.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Données pour le changement de mot de passe de l'utilisateur connecté")
public record ChangePasswordRequest(
    @Schema(description = "Mot de passe actuel", example = "OldP4ss!")
    @NotBlank String currentPassword,

    @Schema(description = "Nouveau mot de passe (8–72 caractères, au moins une majuscule, un chiffre et un caractère spécial). Doit être différent du mot de passe actuel.", example = "NewS3cur3!", minLength = 8, maxLength = 72)
    @NotBlank @Size(min = 8, max = 72)
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).+$",
        message = "must contain at least one uppercase letter, one digit, and one special character"
    )
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