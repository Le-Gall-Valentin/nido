package com.synapsys.api.identity.domain.port.out;

import java.util.UUID;

public interface MfaAdminResetTotpPort {
    void disableTotpIfEnabled(UUID userId);
}