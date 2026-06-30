package com.synapsys.api.authentication.application.port.in;

import com.synapsys.api.authentication.application.dto.VerifyTotpChallengeCommand;
import com.synapsys.api.authentication.domain.model.LoginResult;

public interface VerifyTotpChallengeUseCase {
    LoginResult.Success verify(VerifyTotpChallengeCommand command);
}