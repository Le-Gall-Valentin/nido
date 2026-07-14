package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.application.dto.DisableTotpCommand;

public interface DisableTotpUseCase {
    void disable(DisableTotpCommand command);
}