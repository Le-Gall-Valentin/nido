package com.nido.api.space.application.port.in;

import java.util.UUID;

public interface HandleUserDeletionUseCase {
    /**
     * Supprime l'espace perso du compte, transfère la propriété de ses groupes
     * au plus ancien ADMIN, à défaut au plus ancien MEMBER, et supprime les groupes
     * dont il ne reste aucun successeur possible.
     */
    void handleUserDeletion(UUID userId);
}
