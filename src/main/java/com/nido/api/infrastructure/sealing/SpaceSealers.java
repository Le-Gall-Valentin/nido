package com.nido.api.infrastructure.sealing;

import java.util.UUID;

/** The sealer of a space, by its id: the salt is asked of the space module when the key is not cached. */
@FunctionalInterface
public interface SpaceSealers {
    SpaceSealer forSpace(UUID spaceId);
}
