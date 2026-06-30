package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.AdminResetTotpCommand;

public interface AdminResetTotpUseCase {
    void reset(AdminResetTotpCommand command);
}