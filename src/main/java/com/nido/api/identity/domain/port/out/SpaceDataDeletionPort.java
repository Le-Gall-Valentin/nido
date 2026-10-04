package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface SpaceDataDeletionPort {
    /** @param username the account's name, read before its anonymisation: the mails this causes name it */
    void deleteSpaceData(UUID userId, String username);
}
