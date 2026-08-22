package com.nido.api.space.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Modification d'un groupe")
public record UpdateSpaceRequest(
    @Schema(description = "Nom du groupe", example = "Chez Valentin")
    @NotBlank @Size(max = 80) String name,

    @Schema(description = "Description facultative", example = "Notre appartement à trois")
    @Size(max = 280) String description,

    @Schema(description = "Couleur d'accent, parmi la palette autorisée", example = "#c17a5c")
    @NotBlank @Size(max = 7) String accent,

    @Schema(description = "Glyphe, parmi la liste autorisée", example = "🏡")
    @NotBlank @Size(max = 8) String glyph
) {}
