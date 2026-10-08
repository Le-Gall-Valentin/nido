package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.AdminResetTwoFactorCommand;

public interface AdminResetTwoFactorUseCase {
    void reset(AdminResetTwoFactorCommand command);
}
