package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface UserTotpInitPort {
    void createDefaultRecord(UUID userId);
}