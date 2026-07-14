package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.DeactivateUserCommand;

public interface DeactivateUserUseCase {
    void deactivate(DeactivateUserCommand command);
}