package com.synapsys.api.mfa.application.port.in;

import java.util.UUID;

public interface AdminDisableTotpUseCase {
    void disableIfEnabled(UUID userId);
}