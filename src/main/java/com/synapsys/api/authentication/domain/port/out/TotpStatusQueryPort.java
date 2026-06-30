package com.synapsys.api.authentication.domain.port.out;

import java.util.UUID;

public interface TotpStatusQueryPort {
    boolean isTotpEnabled(UUID userId);
}