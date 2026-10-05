package com.nido.api.identity.domain.port.out;

import java.util.UUID;

public interface MfaAdminResetTotpPort {
    /** @return whether a second factor was on — what makes the reset worth telling */
    boolean disableTotpIfEnabled(UUID userId);
}