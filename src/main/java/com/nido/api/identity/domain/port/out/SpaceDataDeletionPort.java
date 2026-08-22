package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface SpaceDataDeletionPort {
    void deleteSpaceData(UUID userId);
}
