package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Un code à 6 chiffres : de l'application, ou reçu par mail")
public record TwoFactorCodeRequest(
    @Schema(description = "Exactement 6 chiffres", example = "123456", pattern = "\\d{6}")
    @NotBlank @Pattern(regexp = "\\d{6}", message = "must be exactly 6 digits") String code
) {
    @Override
    public String toString() {
        return "TwoFactorCodeRequest[code=***]";
    }
}
