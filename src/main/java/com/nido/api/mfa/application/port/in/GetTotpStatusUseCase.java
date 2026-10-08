package com.nido.api.mfa.application.port.in;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface GetTotpStatusUseCase {
    Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds);
}
