package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.AdminResetTotpCommand;

public interface AdminResetTotpUseCase {
    void reset(AdminResetTotpCommand command);
}