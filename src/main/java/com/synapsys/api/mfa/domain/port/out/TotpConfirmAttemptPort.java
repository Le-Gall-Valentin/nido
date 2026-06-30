package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface TotpConfirmAttemptPort {
    int incrementAndGetAttempts(UUID userId);
    void clearAttempts(UUID userId);
}