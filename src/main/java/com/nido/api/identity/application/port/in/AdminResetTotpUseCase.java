package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.AdminResetTotpCommand;

public interface AdminResetTotpUseCase {
    void reset(AdminResetTotpCommand command);
}