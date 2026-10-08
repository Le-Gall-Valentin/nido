package com.nido.api.mfa.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(description = "La preuve pour désactiver une méthode ; absente pour une méthode en pause")
public record DisableMethodRequest(
    @Schema(description = "Exactement 6 chiffres, ou absent", example = "123456", pattern = "\\d{6}", nullable = true)
    @Pattern(regexp = "\\d{6}", message = "must be exactly 6 digits") String code
) {
    @Override
    public String toString() {
        return "DisableMethodRequest[code=***]";
    }
}
