package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.DeactivateUserCommand;

public interface DeactivateUserUseCase {
    void deactivate(DeactivateUserCommand command);
}