package com.nido.api.space.application.port.in;

import java.util.UUID;

public interface CreatePersonalSpaceUseCase {
    /** Idempotent : retourne l'espace perso existant si le compte en a déjà un. */
    UUID createFor(UUID userId);
}
