package com.nido.api.space.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Réponse paginée générique")
public record PageResponse<T>(
    @Schema(description = "Contenu de la page courante") List<T> content,
    @Schema(description = "Nombre total d'éléments", example = "42") long totalElements,
    @Schema(description = "Index de la page courante", example = "0") int page,
    @Schema(description = "Taille de la page", example = "20") int size
) {}
