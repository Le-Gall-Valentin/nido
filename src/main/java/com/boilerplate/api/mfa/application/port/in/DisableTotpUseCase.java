package com.boilerplate.api.mfa.application.port.in;

import com.boilerplate.api.mfa.application.dto.DisableTotpCommand;

public interface DisableTotpUseCase {
    void disable(DisableTotpCommand command);
}