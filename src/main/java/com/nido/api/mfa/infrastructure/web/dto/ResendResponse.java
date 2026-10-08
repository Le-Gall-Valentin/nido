package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Un code est parti par mail")
public record ResendResponse(
    @Schema(description = "Secondes avant de pouvoir en demander un autre", example = "60") long resendAfterSeconds
) {}
