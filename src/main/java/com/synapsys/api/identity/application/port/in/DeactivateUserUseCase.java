package com.synapsys.api.identity.application.port.in;

import com.synapsys.api.identity.domain.model.DeactivateUserCommand;

public interface DeactivateUserUseCase {
    void deactivate(DeactivateUserCommand command);
}