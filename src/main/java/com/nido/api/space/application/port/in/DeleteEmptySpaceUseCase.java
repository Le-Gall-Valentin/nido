package com.nido.api.space.application.port.in;

import java.util.UUID;

public interface DeleteEmptySpaceUseCase {
    /** Filet de réparation : ne supprime qu'un contexte dont il ne reste aucun membre. */
    void delete(UUID spaceId, UUID callerId);
}
