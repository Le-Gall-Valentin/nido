package com.synapsys.api.identity.domain.port.out;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface TotpStatusPort {
    boolean isTotpEnabled(UUID userId);
    Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds);
}