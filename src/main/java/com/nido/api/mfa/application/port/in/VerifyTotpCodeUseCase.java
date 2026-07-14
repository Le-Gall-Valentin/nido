package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.application.dto.TotpCodeVerifyResult;
import java.util.UUID;

public interface VerifyTotpCodeUseCase {
    TotpCodeVerifyResult verifyAndConsume(UUID userId, String code);
}