package com.nido.api.mfa.application.port.in;

import com.nido.api.mfa.application.dto.ConfirmTotpCommand;

public interface ConfirmTotpUseCase {
    void confirm(ConfirmTotpCommand command);
}