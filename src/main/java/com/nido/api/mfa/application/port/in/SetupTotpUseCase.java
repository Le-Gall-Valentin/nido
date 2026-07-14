package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.application.dto.SetupTotpCommand;
import com.nido.api.mfa.domain.model.TotpSetupResult;

public interface SetupTotpUseCase {
    TotpSetupResult setup(SetupTotpCommand command);
}