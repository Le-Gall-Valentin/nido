package com.nido.api.space.application.port.in;

import java.util.UUID;

/**
 * Lets another bounded context (Finance) fetch a space's stable encryption
 * salt without reaching into space's persistence layer directly — the same
 * cross-BC boundary every other module already crosses through an
 * application port (e.g. {@code ResolveMembershipUseCase}).
 */
public interface GetSpaceEncryptionSaltUseCase {
    String getEncryptionSalt(UUID spaceId);
}
