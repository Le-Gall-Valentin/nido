package com.nido.api.space.application.port.in;

import java.util.UUID;

public interface CreatePersonalSpaceUseCase {
    /**
     * Idempotent sur une reprise séquentielle : retourne l'espace perso existant si le
     * compte en a déjà un. En revanche une vraie course entre deux appels concurrents
     * remonte en erreur : après l'échec du flush la session est inutilisable, on ne
     * peut donc pas la rattraper dans la même transaction.
     */
    UUID createFor(UUID userId);
}
