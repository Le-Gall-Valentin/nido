package com.boilerplate.api.mfa.application.port.in;

import com.boilerplate.api.mfa.application.dto.TotpCodeVerifyResult;
import java.util.UUID;

public interface VerifyTotpCodeUseCase {
    TotpCodeVerifyResult verifyAndConsume(UUID userId, String code);
}