package com.synapsys.api.mfa.application.port.in;

import com.synapsys.api.mfa.application.dto.ConfirmTotpCommand;

public interface ConfirmTotpUseCase {
    void confirm(ConfirmTotpCommand command);
}