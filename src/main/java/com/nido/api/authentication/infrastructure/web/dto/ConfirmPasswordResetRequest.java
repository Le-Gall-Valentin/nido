package com.nido.api.authentication.infrastructure.web.dto;

import com.nido.api.shared.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nouveau mot de passe, avec le jeton du lien")
public record ConfirmPasswordResetRequest(
    @Schema(description = "Le jeton lu après le # du lien", maxLength = 128)
    @NotBlank @Size(max = 128) String token,
    @Schema(description = "Nouveau mot de passe (8 caractères minimum, 72 octets maximum — une lettre accentuée en compte deux —, au moins une majuscule, un chiffre et un caractère spécial)",
        example = "NewS3cur3!", minLength = 8, maxLength = 72)
    @StrongPassword String newPassword
) {
    @Override
    public String toString() {
        return "ConfirmPasswordResetRequest[token=***, newPassword=***]";
    }
}
