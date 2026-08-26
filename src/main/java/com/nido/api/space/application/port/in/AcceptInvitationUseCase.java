package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.AcceptInvitationCommand;

import java.util.UUID;

public interface AcceptInvitationUseCase {
    /** Retourne l'identifiant du contexte rejoint. */
    UUID accept(AcceptInvitationCommand command, UUID userId, String userEmail);

    /**
     * Même flux que {@link #accept}, pour l'invité qui accepte depuis sa liste d'invitations
     * reçues plutôt qu'avec un code reçu hors bande. L'invitation est retrouvée par identifiant
     * au lieu du code, mais passe exactement les mêmes garde-fous, dans le même ordre.
     */
    UUID acceptById(UUID invitationId, UUID userId, String userEmail);
}
