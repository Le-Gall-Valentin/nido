package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Activation du code par mail : le code est parti")
public record MailSetupResponse(
    @Schema(description = "L'adresse du compte, où le code est parti", example = "jane@example.fr") String sentTo,
    @Schema(description = "Secondes avant de pouvoir demander un nouveau code", example = "60") long resendAfterSeconds
) implements SetupResponse {}
