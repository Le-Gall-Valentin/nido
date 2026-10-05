package com.nido.api.identity.infrastructure.web.dto;

import com.nido.api.shared.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Le compte créé, et comment son invitation est partie")
public record RegisteredUserResponse(
    @Schema(description = "Identifiant unique de l'utilisateur") UUID id,
    @Schema(description = "Nom d'utilisateur", example = "jane.doe") String username,
    @Schema(description = "Adresse email", example = "jane.doe@example.com") String email,
    @Schema(description = "Rôle de l'utilisateur") Role role,
    @Schema(description = "Date de création du compte (UTC)") Instant createdAt,
    @Schema(description = "Toujours false à la création", example = "false") boolean totpEnabled,
    @Schema(description = "Toujours null à la création : la langue s'enregistre à la première connexion", nullable = true) String language,
    @Schema(description = "Comment l'invitation a été transmise") InvitationDeliveryResponse invitation
) {}
