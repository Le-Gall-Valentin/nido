package com.synapsys.api.mfa.application.port.in;

import com.synapsys.api.mfa.application.dto.SetupTotpCommand;
import com.synapsys.api.mfa.domain.model.TotpSetupResult;

public interface SetupTotpUseCase {
    TotpSetupResult setup(SetupTotpCommand command);
}