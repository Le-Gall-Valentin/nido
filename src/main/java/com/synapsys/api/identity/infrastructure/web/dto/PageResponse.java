package com.synapsys.api.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Réponse paginée générique")
public record PageResponse<T>(
    @Schema(description = "Contenu de la page courante")
    List<T> content,

    @Schema(description = "Nombre total d'éléments toutes pages confondues", example = "42")
    long totalElements,

    @Schema(description = "Index de la page courante (commence à 0)", example = "0")
    int page,

    @Schema(description = "Taille de la page", example = "20")
    int size
) {}