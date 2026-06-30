package com.synapsys.api.authentication.infrastructure.security;

import com.synapsys.api.authentication.domain.model.TotpVerificationResult;
import com.synapsys.api.authentication.domain.port.out.MfaTotpVerifierPort;
import com.synapsys.api.mfa.application.dto.TotpCodeVerifyResult;
import com.synapsys.api.mfa.application.port.in.VerifyTotpCodeUseCase;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class MfaTotpVerifierAdapter implements MfaTotpVerifierPort {

    private final VerifyTotpCodeUseCase verificationService;

    public MfaTotpVerifierAdapter(VerifyTotpCodeUseCase verificationService) {
        this.verificationService = verificationService;
    }

    @Override
    public TotpVerificationResult verifyAndConsume(UUID userId, String code) {
        TotpCodeVerifyResult result = verificationService.verifyAndConsume(userId, code);
        return switch (result) {
            case SUCCESS -> TotpVerificationResult.SUCCESS;
            case REPLAYED -> TotpVerificationResult.REPLAYED;
            case INVALID -> TotpVerificationResult.INVALID;
        };
    }
}