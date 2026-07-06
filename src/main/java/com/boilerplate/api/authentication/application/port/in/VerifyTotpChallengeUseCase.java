package com.boilerplate.api.authentication.application.port.in;

import com.boilerplate.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.boilerplate.api.authentication.domain.model.LoginResult;

public interface VerifyTotpChallengeUseCase {
    LoginResult.Success verify(VerifyTotpChallengeCommand command);
}