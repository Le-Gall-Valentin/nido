package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.validation.LanguageCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Langue dans laquelle l'utilisateur lit l'application")
public record UpdateLanguageRequest(
    @Schema(description = "Code de langue", example = "fr", allowableValues = {"fr", "en"})
    @NotNull @LanguageCode String language
) {}
