package com.nido.api.identity.application.port.in;

import com.nido.api.identity.domain.model.ActivateUserCommand;

public interface ActivateUserUseCase {
    void activate(ActivateUserCommand command);
}