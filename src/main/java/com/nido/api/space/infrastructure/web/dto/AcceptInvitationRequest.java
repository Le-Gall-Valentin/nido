package com.nido.api.space.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aucune annotation de validation ici, volontairement : un code vide, trop long ou inconnu
 * doivent produire exactement la même réponse. Bean Validation s'exécuterait avant le domaine
 * et rendrait un 400 distinguable du 404. La normalisation et le refus appartiennent à
 * AcceptInvitationCommand, qui lève InvitationNotFound.
 */
@Schema(description = "Le code d'invitation reçu par l'invité")
public record AcceptInvitationRequest(
    @Schema(description = "Code d'invitation", example = "NIDO-ABC123")
    String code
) {}
