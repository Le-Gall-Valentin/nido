package com.nido.api.authentication.application.port.in;

import com.nido.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.nido.api.authentication.domain.model.LoginResult;

public interface VerifyTotpChallengeUseCase {
    LoginResult.Success verify(VerifyTotpChallengeCommand command);
}