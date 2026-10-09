package com.nido.api.infrastructure.sealing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The legacy opener of a space, by its id: the salt is asked of the space module at each call. */
@FunctionalInterface
public interface LegacySpaceOpeners {

    LegacySpaceOpener forSpace(UUID spaceId);

    /**
     * The same openers, each derived once for as long as the returned instance is kept — one run of the migration or of
     * the key check, on one thread. Nothing legacy is cached beyond it.
     */
    static LegacySpaceOpeners remembering(LegacySpaceOpeners openers) {
        Map<UUID, LegacySpaceOpener> known = new HashMap<>();
        return spaceId -> known.computeIfAbsent(spaceId, openers::forSpace);
    }
}
