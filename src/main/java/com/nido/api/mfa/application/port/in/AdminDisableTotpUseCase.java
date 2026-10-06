package com.nido.api.mfa.application.port.in;

import java.util.UUID;

public interface AdminDisableTotpUseCase {
    /** @return whether a second factor was on */
    boolean disableIfEnabled(UUID userId);
}