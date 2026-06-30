package com.synapsys.api.authentication.domain.port.out;

import java.util.UUID;

public interface RefreshTokenRevocationPort {
    boolean tryMarkUsedAndRevoke(UUID tokenId);
    void revoke(UUID tokenId);
    void revokeAllForUser(UUID userId);
    void deleteAllForUser(UUID userId);
}