package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface UserTotpSetupPort {
    boolean saveTotpSecretIfAbsent(UUID userId, String secret);
    void clearPendingSecret(UUID userId);
}