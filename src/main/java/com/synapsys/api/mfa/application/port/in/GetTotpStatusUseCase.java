package com.synapsys.api.mfa.application.port.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface GetTotpStatusUseCase {
    boolean isTotpEnabled(UUID userId);
    Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds);
}
