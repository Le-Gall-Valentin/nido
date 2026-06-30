package com.synapsys.api.mfa.application.port.in;

import com.synapsys.api.mfa.application.dto.TotpCodeVerifyResult;
import java.util.UUID;

public interface VerifyTotpCodeUseCase {
    TotpCodeVerifyResult verifyAndConsume(UUID userId, String code);
}