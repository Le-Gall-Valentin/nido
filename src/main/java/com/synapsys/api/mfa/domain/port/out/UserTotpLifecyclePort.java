package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface UserTotpLifecyclePort {
    void enableTotp(UUID userId);
    void disableTotp(UUID userId);
    void deleteTotp(UUID userId);
}