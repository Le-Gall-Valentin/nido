package com.synapsys.api.mfa.application.port.in;

import com.synapsys.api.mfa.application.dto.DisableTotpCommand;

public interface DisableTotpUseCase {
    void disable(DisableTotpCommand command);
}