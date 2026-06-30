package com.synapsys.api.mfa.application.service;

import com.synapsys.api.mfa.application.port.in.VerifyTotpCodeUseCase;
import com.synapsys.api.mfa.application.dto.TotpCodeVerifyResult;
import com.synapsys.api.mfa.domain.model.UserTotpProfile;
import com.synapsys.api.mfa.domain.port.out.*;
import com.synapsys.api.shared.annotation.ApplicationService;
import java.util.Optional;
import java.util.UUID;

@ApplicationService
public class TotpCodeVerificationService implements VerifyTotpCodeUseCase {

    private final UserTotpQueryPort userTotpQuery;
    private final TotpCodeValidatorPort codeValidator;
    private final TotpCodeReplayPort codeReplay;

    public TotpCodeVerificationService(UserTotpQueryPort userTotpQuery,
                                       TotpCodeValidatorPort codeValidator,
                                       TotpCodeReplayPort codeReplay) {
        this.userTotpQuery = userTotpQuery;
        this.codeValidator = codeValidator;
        this.codeReplay = codeReplay;
    }

    public TotpCodeVerifyResult verifyAndConsume(UUID userId, String code) {
        Optional<UserTotpProfile> profileOpt = userTotpQuery.findById(userId);
        if (profileOpt.isEmpty()) return TotpCodeVerifyResult.INVALID;
        UserTotpProfile profile = profileOpt.get();
        if (!profile.totpEnabled()) return TotpCodeVerifyResult.INVALID;
        if (profile.totpSecret().isEmpty()) return TotpCodeVerifyResult.INVALID;
        if (!codeValidator.isValid(profile.totpSecret().get(), code)) return TotpCodeVerifyResult.INVALID;
        return codeReplay.markCodeUsedIfAbsent(userId, code)
            ? TotpCodeVerifyResult.SUCCESS
            : TotpCodeVerifyResult.REPLAYED;
    }
}