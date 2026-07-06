package com.boilerplate.api.mfa.application.port.in;

import com.boilerplate.api.mfa.application.dto.ConfirmTotpCommand;

public interface ConfirmTotpUseCase {
    void confirm(ConfirmTotpCommand command);
}