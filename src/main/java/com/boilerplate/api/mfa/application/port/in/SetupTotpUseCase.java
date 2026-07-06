package com.boilerplate.api.mfa.application.port.in;

import com.boilerplate.api.mfa.application.dto.SetupTotpCommand;
import com.boilerplate.api.mfa.domain.model.TotpSetupResult;

public interface SetupTotpUseCase {
    TotpSetupResult setup(SetupTotpCommand command);
}