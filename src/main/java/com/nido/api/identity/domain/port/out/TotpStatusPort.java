package com.nido.api.identity.domain.port.out;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface TotpStatusPort {
    Set<UUID> findTotpEnabledAmong(Collection<UUID> userIds);
}