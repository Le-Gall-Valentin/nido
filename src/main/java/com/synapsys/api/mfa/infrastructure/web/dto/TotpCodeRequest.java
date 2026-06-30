package com.synapsys.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Code TOTP à 6 chiffres généré par l'application d'authentification")
public record TotpCodeRequest(
    @Schema(description = "Code TOTP (exactement 6 chiffres)", example = "123456", pattern = "\\d{6}")
    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "must be exactly 6 digits")
    String code
) {}