package com.boilerplate.api.identity.application.port.in;

import com.boilerplate.api.identity.domain.model.ActivateUserCommand;

public interface ActivateUserUseCase {
    void activate(ActivateUserCommand command);
}