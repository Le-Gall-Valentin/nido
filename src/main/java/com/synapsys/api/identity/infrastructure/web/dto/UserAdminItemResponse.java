package com.synapsys.api.identity.infrastructure.web.dto;

import com.synapsys.api.shared.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vue administrative d'un utilisateur dans la liste paginée")
public record UserAdminItemResponse(
    @Schema(description = "Identifiant unique de l'utilisateur", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Nom d'utilisateur", example = "jane.doe")
    String username,

    @Schema(description = "Adresse email", example = "jane.doe@example.com")
    String email,

    @Schema(description = "Rôle de l'utilisateur")
    Role role,

    @Schema(description = "Indique si le compte est actif", example = "true")
    boolean isActive,

    @Schema(description = "Date de création du compte (UTC)", example = "2024-01-15T10:30:00Z")
    Instant createdAt,

    @Schema(description = "Indique si l'authentification à deux facteurs est activée", example = "false")
    boolean totpEnabled
) {}