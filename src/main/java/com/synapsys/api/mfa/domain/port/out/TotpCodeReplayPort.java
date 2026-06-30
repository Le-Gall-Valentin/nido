package com.synapsys.api.mfa.domain.port.out;

import java.util.UUID;

public interface TotpCodeReplayPort {
    boolean markCodeUsedIfAbsent(UUID userId, String code);
}