package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.VerifyTwoFactorChallengeCommand;
import com.nido.api.authentication.domain.model.LoginResult;

public interface VerifyTwoFactorChallengeUseCase {
    LoginResult.Success verify(VerifyTwoFactorChallengeCommand command);
}