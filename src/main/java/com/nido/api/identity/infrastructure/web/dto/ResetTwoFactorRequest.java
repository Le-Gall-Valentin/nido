package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.TwoFactorMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

@Schema(description = "Les méthodes à retirer, cochées par l'administrateur")
public record ResetTwoFactorRequest(
    @Schema(description = "APP, MAIL ou les deux ; au moins une", example = "[\"APP\"]")
    @NotEmpty Set<TwoFactorMethod> methods
) {}
