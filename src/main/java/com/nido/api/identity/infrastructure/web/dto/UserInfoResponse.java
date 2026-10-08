package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.Role;
import com.nido.api.shared.model.TwoFactorMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Intentional duplication: authentication BC has an identical record. Keep both in sync.
@Schema(description = "Informations de l'utilisateur authentifié")
public record UserInfoResponse(
    @Schema(description = "Identifiant unique de l'utilisateur", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Nom d'utilisateur", example = "john.doe")
    String username,

    @Schema(description = "Adresse email", example = "john.doe@example.com")
    String email,

    @Schema(description = "Rôle de l'utilisateur")
    Role role,

    @Schema(description = "Date de création du compte (UTC)", example = "2024-01-15T10:30:00Z")
    Instant createdAt,

    @Schema(description = "Les méthodes de double authentification activées, en pause comprises (APP, MAIL)")
    List<TwoFactorMethod> twoFactorMethods,

    @Schema(description = "Langue du compte (fr, en), ou null tant qu'aucune n'a été enregistrée", example = "fr", nullable = true)
    String language
) {}