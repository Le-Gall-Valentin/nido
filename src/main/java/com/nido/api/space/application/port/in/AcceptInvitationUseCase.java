package com.nido.api.space.application.port.in;

import com.nido.api.space.domain.model.AcceptInvitationCommand;

import java.util.UUID;

public interface AcceptInvitationUseCase {
    /** Retourne l'identifiant du contexte rejoint. */
    UUID accept(AcceptInvitationCommand command, UUID userId, String userEmail);
}
